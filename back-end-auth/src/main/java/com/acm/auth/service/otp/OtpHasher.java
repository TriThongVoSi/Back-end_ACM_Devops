package com.acm.auth.service.otp;

public interface OtpHasher {
    String hash(String otp);

    boolean matches(String otp, String hash);
}
