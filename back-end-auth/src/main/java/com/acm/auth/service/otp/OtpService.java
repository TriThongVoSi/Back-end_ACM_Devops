package com.acm.auth.service.otp;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acm.auth.entity.OtpVerification;
import com.acm.auth.enums.OtpPurpose;
import com.acm.auth.exception.AppException;
import com.acm.auth.exception.ErrorCode;
import com.acm.auth.repository.OtpVerificationRepository;
import com.acm.auth.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpVerificationRepository otpVerificationRepository;
    private final OtpGenerator otpGenerator;
    private final OtpHasher otpHasher;
    private final EmailService emailService;
    private final Clock clock;

    @Value("${otp.expiry-minutes:5}")
    private long expiryMinutes;

    @Value("${otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${otp.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    @Transactional
    public OtpChallenge sendOtp(String email, Long userId, OtpPurpose purpose, boolean enforceCooldown) {
        LocalDateTime now = LocalDateTime.now(clock);

        var existing = otpVerificationRepository
                .findTopByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(email, purpose);

        if (existing.isPresent()) {
            OtpVerification active = existing.get();
            if (enforceCooldown && active.getLastSentAt() != null) {
                LocalDateTime allowedTime = active.getLastSentAt().plusSeconds(resendCooldownSeconds);
                if (allowedTime.isAfter(now)) {
                    throw new AppException(ErrorCode.OTP_RESEND_TOO_SOON);
                }
            }

            active.setConsumedAt(now);
            otpVerificationRepository.save(active);
        }

        String otp = otpGenerator.generate();
        String otpHash = otpHasher.hash(otp);

        LocalDateTime expiresAt = now.plusMinutes(expiryMinutes);
        int resendCount = existing.map(OtpVerification::getResendCount).orElse(0) + 1;

        OtpVerification verification = OtpVerification.builder()
                .email(email)
                .userId(userId)
                .purpose(purpose)
                .otpHash(otpHash)
                .createdAt(now)
                .expiresAt(expiresAt)
                .attempts(0)
                .maxAttempts(maxAttempts)
                .lastSentAt(now)
                .resendCount(resendCount)
                .build();

        otpVerificationRepository.save(verification);
        emailService.sendOtpEmail(email, otp, purpose, ChronoUnit.SECONDS.between(now, expiresAt));

        return new OtpChallenge(maskEmail(email), ChronoUnit.SECONDS.between(now, expiresAt));
    }

    @Transactional
    public void verifyOtp(String email, OtpPurpose purpose, String otp) {
        LocalDateTime now = LocalDateTime.now(clock);

        OtpVerification verification = otpVerificationRepository
                .findTopByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new AppException(ErrorCode.OTP_INVALID));

        if (verification.getExpiresAt() != null && verification.getExpiresAt().isBefore(now)) {
            verification.setConsumedAt(now);
            otpVerificationRepository.save(verification);
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        if (verification.getAttempts() >= verification.getMaxAttempts()) {
            verification.setConsumedAt(now);
            otpVerificationRepository.save(verification);
            throw new AppException(ErrorCode.OTP_TOO_MANY_ATTEMPTS);
        }

        boolean match = otpHasher.matches(otp, verification.getOtpHash());
        if (!match) {
            int attempts = verification.getAttempts() + 1;
            verification.setAttempts(attempts);
            if (attempts >= verification.getMaxAttempts()) {
                verification.setConsumedAt(now);
                otpVerificationRepository.save(verification);
                throw new AppException(ErrorCode.OTP_TOO_MANY_ATTEMPTS);
            }
            otpVerificationRepository.save(verification);
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        verification.setConsumedAt(now);
        otpVerificationRepository.save(verification);
        log.info("OTP verified successfully for email {} and purpose {}", email, purpose);
    }

    public long getExpirySeconds() {
        return expiryMinutes * 60;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) {
            return "*@%s".formatted(domain);
        }
        return "%s***%s@%s".formatted(local.charAt(0), local.charAt(local.length() - 1), domain);
    }
}
