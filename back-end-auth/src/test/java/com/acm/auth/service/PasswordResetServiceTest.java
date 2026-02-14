package com.acm.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.acm.auth.dto.request.ForgotPasswordVerifyOtpRequest;
import com.acm.auth.dto.request.ResetPasswordRequest;
import com.acm.auth.dto.response.ForgotPasswordVerifyOtpResponse;
import com.acm.auth.dto.response.ResetPasswordResponse;
import com.acm.auth.entity.User;
import com.acm.auth.enums.UserStatus;
import com.acm.auth.repository.UserRepository;
import com.acm.auth.service.ResetTokenService.ResetTokenPayload;
import com.acm.auth.service.otp.OtpService;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpService otpService;

    @Mock
    private ResetTokenService resetTokenService;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setup() {
        passwordResetService = new PasswordResetService(userRepository, passwordEncoder, otpService, resetTokenService);
    }

    @Test
    void verifyOtpIssuesResetToken() {
        User user = User.builder()
                .id(10L)
                .email("user@example.com")
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(resetTokenService.issueToken(10L, "user@example.com")).thenReturn("temp-token");
        when(resetTokenService.getValidSeconds()).thenReturn(600L);

        ForgotPasswordVerifyOtpResponse response = passwordResetService.verifyOtp(
                ForgotPasswordVerifyOtpRequest.builder()
                        .email("user@example.com")
                        .otp("123456")
                        .build());

        assertEquals("temp-token", response.getTempResetToken());
        assertEquals(600L, response.getExpiresInSeconds());
        verify(otpService, times(1)).verifyOtp("user@example.com", com.acm.auth.enums.OtpPurpose.RESET_PASSWORD, "123456");
    }

    @Test
    void resetPasswordUpdatesPasswordAndInvalidatesToken() {
        User user = User.builder()
                .id(10L)
                .email("user@example.com")
                .status(UserStatus.ACTIVE)
                .build();

        ResetTokenPayload payload = new ResetTokenPayload("jwt-id", "user@example.com", 10L, new Date());
        when(resetTokenService.verifyToken("temp-token")).thenReturn(payload);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResetPasswordResponse response = passwordResetService.resetPassword(
                ResetPasswordRequest.builder()
                        .tempResetToken("temp-token")
                        .newPassword("newPassword")
                        .build());

        assertNotNull(response);
        assertEquals("hashed", user.getPassword());
        verify(resetTokenService, times(1)).invalidateToken(eq("jwt-id"), any(Date.class));
    }
}
