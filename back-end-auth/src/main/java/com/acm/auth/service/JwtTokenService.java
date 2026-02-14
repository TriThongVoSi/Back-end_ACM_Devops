package com.acm.auth.service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.extern.slf4j.Slf4j;
import com.acm.auth.entity.InvalidatedToken;
import com.acm.auth.entity.User;
import com.acm.auth.exception.AppException;
import com.acm.auth.exception.ErrorCode;
import com.acm.auth.repository.InvalidatedTokenRepository;

@Service
@Slf4j
public class JwtTokenService {

    @Value("${jwt.signer-key}")
    private String signerKey;

    @Value("${jwt.valid-duration}")
    private long validDuration;

    @Value("${jwt.refreshable-duration}")
    private long refreshableDuration;

    private final InvalidatedTokenRepository invalidatedTokenRepository;

    public JwtTokenService(InvalidatedTokenRepository invalidatedTokenRepository) {
        this.invalidatedTokenRepository = invalidatedTokenRepository;
    }

    public long getValidDuration() {
        return validDuration;
    }

    /**
     * Generate JWT token for authenticated user.
     */
    public String generateToken(User user, String primaryRole) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getEmail())
                .issuer("auth-service")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(validDuration, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("user_id", user.getId())
                .claim("email", user.getEmail())
                .claim("username", user.getUsername())
                .claim("role", primaryRole)
                .claim("scope", buildScope(user))
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(signerKey.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Verify JWT token.
     */
    public SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(signerKey.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = isRefresh
                ? new Date(signedJWT.getJWTClaimsSet().getIssueTime().toInstant()
                        .plus(refreshableDuration, ChronoUnit.SECONDS).toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        boolean verified = signedJWT.verify(verifier);

        if (!(verified && expiryTime.after(new Date()))) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return signedJWT;
    }

    /**
     * Invalidate token for logout.
     */
    public void invalidateToken(String jwtId, Date expiryTime) {
        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jwtId)
                .expiryTime(expiryTime)
                .build();
        invalidatedTokenRepository.save(invalidatedToken);
    }

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");

        if (!CollectionUtils.isEmpty(user.getRoles())) {
            user.getRoles().forEach(role -> stringJoiner.add("ROLE_" + role.getCode()));
        }

        return stringJoiner.toString();
    }
}
