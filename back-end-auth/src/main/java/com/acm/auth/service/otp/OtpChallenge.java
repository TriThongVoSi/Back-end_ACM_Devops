package com.acm.auth.service.otp;

public record OtpChallenge(String emailMasked, long expiresInSeconds) {
}
