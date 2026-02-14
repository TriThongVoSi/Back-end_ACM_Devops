package com.acm.auth.service;

import com.acm.auth.enums.OtpPurpose;

public interface EmailService {
    void sendOtpEmail(String to, String otp, OtpPurpose purpose, long expiresInSeconds);
}
