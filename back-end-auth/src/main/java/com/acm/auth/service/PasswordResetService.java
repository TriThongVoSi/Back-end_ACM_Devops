package com.acm.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acm.auth.dto.request.ForgotPasswordRequest;
import com.acm.auth.dto.request.ForgotPasswordVerifyOtpRequest;
import com.acm.auth.dto.request.ResetPasswordRequest;
import com.acm.auth.dto.response.ForgotPasswordVerifyOtpResponse;
import com.acm.auth.dto.response.ResetPasswordResponse;
import com.acm.auth.dto.response.OtpChallengeResponse;
import com.acm.auth.entity.User;
import com.acm.auth.enums.OtpPurpose;
import com.acm.auth.enums.UserStatus;
import com.acm.auth.exception.AppException;
import com.acm.auth.exception.ErrorCode;
import com.acm.auth.repository.UserRepository;
import com.acm.auth.service.ResetTokenService.ResetTokenPayload;
import com.acm.auth.service.otp.OtpService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final ResetTokenService resetTokenService;

    @Transactional
    public OtpChallengeResponse requestReset(ForgotPasswordRequest request) {
        String email = request.getEmail();

        userRepository.findByEmail(email)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .ifPresent(user -> {
                    try {
                        otpService.sendOtp(user.getEmail(), user.getId(), OtpPurpose.RESET_PASSWORD, true);
                    } catch (AppException ex) {
                        log.info("OTP reset request throttled for {}", email);
                    }
                });

        return OtpChallengeResponse.builder()
                .message("If account exists, OTP has been sent")
                .expiresInSeconds(otpService.getExpirySeconds())
                .build();
    }

    @Transactional
    public ForgotPasswordVerifyOtpResponse verifyOtp(ForgotPasswordVerifyOtpRequest request) {
        otpService.verifyOtp(request.getEmail(), OtpPurpose.RESET_PASSWORD, request.getOtp());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.OTP_INVALID));

        String token = resetTokenService.issueToken(user.getId(), user.getEmail());
        return ForgotPasswordVerifyOtpResponse.builder()
                .tempResetToken(token)
                .expiresInSeconds(resetTokenService.getValidSeconds())
                .build();
    }

    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        ResetTokenPayload payload = resetTokenService.verifyToken(request.getTempResetToken());

        User user = null;
        if (payload.userId() != null) {
            user = userRepository.findById(payload.userId()).orElse(null);
        }
        if (user == null && payload.email() != null) {
            user = userRepository.findByEmail(payload.email()).orElse(null);
        }
        if (user == null) {
            throw new AppException(ErrorCode.RESET_TOKEN_INVALID);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        resetTokenService.invalidateToken(payload.jwtId(), payload.expiresAt());

        return ResetPasswordResponse.builder()
                .message("Password updated successfully")
                .build();
    }
}
