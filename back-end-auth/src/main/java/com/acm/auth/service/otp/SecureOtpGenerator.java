package com.acm.auth.service.otp;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class SecureOtpGenerator implements OtpGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        int value = secureRandom.nextInt(1_000_000);
        return String.format("%06d", value);
    }
}
