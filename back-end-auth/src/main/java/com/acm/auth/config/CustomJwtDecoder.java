package com.acm.auth.config;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;

import lombok.extern.slf4j.Slf4j;
import com.acm.auth.repository.InvalidatedTokenRepository;

@Component
@Slf4j
public class CustomJwtDecoder implements JwtDecoder {

    @Value("${jwt.signer-key}")
    private String signerKey;

    private final InvalidatedTokenRepository invalidatedTokenRepository;

    public CustomJwtDecoder(InvalidatedTokenRepository invalidatedTokenRepository) {
        this.invalidatedTokenRepository = invalidatedTokenRepository;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            JWSVerifier verifier = new MACVerifier(signerKey.getBytes());
            boolean verified = signedJWT.verify(verifier);

            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            boolean notExpired = expirationTime != null && expirationTime.after(new Date());

            if (!verified || !notExpired) {
                throw new JwtException("Invalid JWT token");
            }

            String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
            if (jwtId != null && invalidatedTokenRepository.existsById(jwtId)) {
                throw new JwtException("Token has been invalidated");
            }

            Instant issuedAt = signedJWT.getJWTClaimsSet().getIssueTime() != null
                    ? signedJWT.getJWTClaimsSet().getIssueTime().toInstant()
                    : Instant.now();
            Instant expiresAt = expirationTime.toInstant();

            return new Jwt(
                    token,
                    issuedAt,
                    expiresAt,
                    signedJWT.getHeader().toJSONObject(),
                    signedJWT.getJWTClaimsSet().getClaims());

        } catch (ParseException | JOSEException e) {
            log.error("Failed to decode JWT: {}", e.getMessage());
            throw new JwtException("Failed to decode JWT token", e);
        }
    }
}
