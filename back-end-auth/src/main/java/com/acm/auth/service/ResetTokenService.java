package com.acm.auth.service;

import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.acm.auth.entity.InvalidatedToken;
import com.acm.auth.exception.AppException;
import com.acm.auth.exception.ErrorCode;
import com.acm.auth.repository.InvalidatedTokenRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResetTokenService {

    private static final String PURPOSE = "RESET_PASSWORD";

    private final InvalidatedTokenRepository invalidatedTokenRepository;
    private final Clock clock;

    @Value("${jwt.signer-key}")
    private String signerKey;

    @Value("${reset-token.valid-minutes:10}")
    private long validMinutes;

    public String issueToken(Long userId, String email) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(validMinutes, ChronoUnit.MINUTES);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(email)
                .issuer("auth-service")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiresAt))
                .jwtID(UUID.randomUUID().toString())
                .claim("purpose", PURPOSE)
                .claim("user_id", userId)
                .claim("email", email)
                .build();

        JWSObject jwsObject = new JWSObject(header, new Payload(claimsSet.toJSONObject()));

        try {
            jwsObject.sign(new MACSigner(signerKey.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign reset token", e);
        }
    }

    public ResetTokenPayload verifyToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            boolean verified = signedJWT.verify(new MACVerifier(signerKey.getBytes()));
            if (!verified) {
                throw new AppException(ErrorCode.RESET_TOKEN_INVALID);
            }

            Date expiry = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expiry == null || expiry.before(new Date())) {
                throw new AppException(ErrorCode.RESET_TOKEN_EXPIRED);
            }

            String purpose = signedJWT.getJWTClaimsSet().getStringClaim("purpose");
            if (!PURPOSE.equals(purpose)) {
                throw new AppException(ErrorCode.RESET_TOKEN_INVALID);
            }

            String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
            if (jwtId != null && invalidatedTokenRepository.existsById(jwtId)) {
                throw new AppException(ErrorCode.RESET_TOKEN_INVALID);
            }

            String email = signedJWT.getJWTClaimsSet().getStringClaim("email");
            if (email == null || email.isBlank()) {
                throw new AppException(ErrorCode.RESET_TOKEN_INVALID);
            }
            Long userId = null;
            Object userIdClaim = signedJWT.getJWTClaimsSet().getClaim("user_id");
            if (userIdClaim instanceof Number num) {
                userId = num.longValue();
            } else if (userIdClaim instanceof String str) {
                try {
                    userId = Long.parseLong(str);
                } catch (NumberFormatException ex) {
                    userId = null;
                }
            }

            return new ResetTokenPayload(jwtId, email, userId, expiry);
        } catch (ParseException | JOSEException e) {
            throw new AppException(ErrorCode.RESET_TOKEN_INVALID);
        }
    }

    public long getValidSeconds() {
        return validMinutes * 60;
    }

    public void invalidateToken(String jwtId, Date expiryTime) {
        if (jwtId == null || expiryTime == null) {
            return;
        }
        invalidatedTokenRepository.save(InvalidatedToken.builder()
                .id(jwtId)
                .expiryTime(expiryTime)
                .build());
    }

    public record ResetTokenPayload(String jwtId, String email, Long userId, Date expiresAt) {
    }
}
