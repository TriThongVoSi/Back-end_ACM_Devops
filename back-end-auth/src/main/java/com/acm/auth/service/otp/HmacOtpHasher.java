package com.acm.auth.service.otp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HmacOtpHasher implements OtpHasher {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] secretBytes;

    public HmacOtpHasher(@Value("${otp.hash-secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("OTP hash secret is missing. Please set otp.hash-secret or OTP_HASH_SECRET.");
        }
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String hash(String otp) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(otp.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash OTP", e);
        }
    }

    @Override
    public boolean matches(String otp, String hash) {
        if (hash == null) {
            return false;
        }
        String computed = hash(otp);
        byte[] expectedBytes = Base64.getDecoder().decode(hash);
        byte[] actualBytes = Base64.getDecoder().decode(computed);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }
}
