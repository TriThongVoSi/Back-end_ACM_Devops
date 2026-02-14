package com.acm.auth.service.otp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.acm.auth.entity.OtpVerification;
import com.acm.auth.enums.OtpPurpose;
import com.acm.auth.exception.AppException;
import com.acm.auth.exception.ErrorCode;
import com.acm.auth.repository.OtpVerificationRepository;
import com.acm.auth.service.EmailService;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpVerificationRepository otpVerificationRepository;

    @Mock
    private OtpGenerator otpGenerator;

    @Mock
    private OtpHasher otpHasher;

    @Mock
    private EmailService emailService;

    private Clock clock;
    private OtpService otpService;

    @BeforeEach
    void setup() {
        clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        otpService = new OtpService(otpVerificationRepository, otpGenerator, otpHasher, emailService, clock);

        ReflectionTestUtils.setField(otpService, "expiryMinutes", 5L);
        ReflectionTestUtils.setField(otpService, "maxAttempts", 5);
        ReflectionTestUtils.setField(otpService, "resendCooldownSeconds", 60L);
    }

    @Test
    void sendOtpInvalidatesOldOtp() {
        LocalDateTime now = LocalDateTime.now(clock);
        OtpVerification existing = OtpVerification.builder()
                .email("test@example.com")
                .purpose(OtpPurpose.REGISTER)
                .otpHash("old-hash")
                .createdAt(now.minusMinutes(1))
                .expiresAt(now.plusMinutes(4))
                .attempts(0)
                .maxAttempts(5)
                .lastSentAt(now.minusSeconds(120))
                .resendCount(1)
                .build();

        when(otpVerificationRepository
                .findTopByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc("test@example.com", OtpPurpose.REGISTER))
                .thenReturn(java.util.Optional.of(existing));
        when(otpGenerator.generate()).thenReturn("123456");
        when(otpHasher.hash("123456")).thenReturn("new-hash");
        when(otpVerificationRepository.save(any(OtpVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        otpService.sendOtp("test@example.com", 1L, OtpPurpose.REGISTER, true);

        assertNotNull(existing.getConsumedAt());
        verify(emailService, times(1)).sendOtpEmail(eq("test@example.com"), eq("123456"), eq(OtpPurpose.REGISTER), anyLong());
    }

    @Test
    void verifyOtpSuccessConsumesOtp() {
        LocalDateTime now = LocalDateTime.now(clock);
        OtpVerification otp = OtpVerification.builder()
                .email("test@example.com")
                .purpose(OtpPurpose.REGISTER)
                .otpHash("hash")
                .createdAt(now.minusMinutes(1))
                .expiresAt(now.plusMinutes(4))
                .attempts(0)
                .maxAttempts(5)
                .build();

        when(otpVerificationRepository
                .findTopByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc("test@example.com", OtpPurpose.REGISTER))
                .thenReturn(java.util.Optional.of(otp));
        when(otpHasher.matches("123456", "hash")).thenReturn(true);
        when(otpVerificationRepository.save(any(OtpVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        otpService.verifyOtp("test@example.com", OtpPurpose.REGISTER, "123456");

        assertNotNull(otp.getConsumedAt());
        assertEquals(0, otp.getAttempts());
    }

    @Test
    void verifyOtpInvalidIncrementsAttemptsAndLocks() {
        LocalDateTime now = LocalDateTime.now(clock);
        OtpVerification otp = OtpVerification.builder()
                .email("test@example.com")
                .purpose(OtpPurpose.REGISTER)
                .otpHash("hash")
                .createdAt(now.minusMinutes(1))
                .expiresAt(now.plusMinutes(4))
                .attempts(4)
                .maxAttempts(5)
                .build();

        when(otpVerificationRepository
                .findTopByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc("test@example.com", OtpPurpose.REGISTER))
                .thenReturn(java.util.Optional.of(otp));
        when(otpHasher.matches("123456", "hash")).thenReturn(false);
        when(otpVerificationRepository.save(any(OtpVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppException ex = assertThrows(AppException.class,
                () -> otpService.verifyOtp("test@example.com", OtpPurpose.REGISTER, "123456"));
        assertEquals(ErrorCode.OTP_TOO_MANY_ATTEMPTS, ex.getErrorCode());
        assertEquals(5, otp.getAttempts());
        assertNotNull(otp.getConsumedAt());
    }

    @Test
    void verifyOtpExpiredThrows() {
        LocalDateTime now = LocalDateTime.now(clock);
        OtpVerification otp = OtpVerification.builder()
                .email("test@example.com")
                .purpose(OtpPurpose.REGISTER)
                .otpHash("hash")
                .createdAt(now.minusMinutes(10))
                .expiresAt(now.minusMinutes(1))
                .attempts(0)
                .maxAttempts(5)
                .build();

        when(otpVerificationRepository
                .findTopByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc("test@example.com", OtpPurpose.REGISTER))
                .thenReturn(java.util.Optional.of(otp));
        when(otpVerificationRepository.save(any(OtpVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppException ex = assertThrows(AppException.class,
                () -> otpService.verifyOtp("test@example.com", OtpPurpose.REGISTER, "123456"));
        assertEquals(ErrorCode.OTP_EXPIRED, ex.getErrorCode());
        assertNotNull(otp.getConsumedAt());
    }

    @Test
    void sendOtpRespectsCooldown() {
        LocalDateTime now = LocalDateTime.now(clock);
        OtpVerification existing = OtpVerification.builder()
                .email("test@example.com")
                .purpose(OtpPurpose.REGISTER)
                .otpHash("old-hash")
                .createdAt(now.minusSeconds(30))
                .expiresAt(now.plusMinutes(4))
                .attempts(0)
                .maxAttempts(5)
                .lastSentAt(now.minusSeconds(10))
                .resendCount(1)
                .build();

        when(otpVerificationRepository
                .findTopByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc("test@example.com", OtpPurpose.REGISTER))
                .thenReturn(java.util.Optional.of(existing));

        AppException ex = assertThrows(AppException.class,
                () -> otpService.sendOtp("test@example.com", 1L, OtpPurpose.REGISTER, true));
        assertEquals(ErrorCode.OTP_RESEND_TOO_SOON, ex.getErrorCode());
    }
}
