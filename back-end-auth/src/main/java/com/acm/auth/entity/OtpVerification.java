package com.acm.auth.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.acm.auth.enums.OtpPurpose;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "otp_verifications", indexes = {
        @Index(name = "idx_otp_email_purpose", columnList = "email,purpose"),
        @Index(name = "idx_otp_user_purpose", columnList = "user_id,purpose"),
        @Index(name = "idx_otp_expires_at", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "otp_id", nullable = false)
    UUID id;

    @Column(name = "user_id")
    Long userId;

    @Column(name = "email", nullable = false, length = 320)
    String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    OtpPurpose purpose;

    @Column(name = "otp_hash", nullable = false, length = 255)
    String otpHash;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    LocalDateTime expiresAt;

    @Column(name = "attempts", nullable = false)
    int attempts;

    @Column(name = "max_attempts", nullable = false)
    int maxAttempts;

    @Column(name = "consumed_at")
    LocalDateTime consumedAt;

    @Column(name = "last_sent_at")
    LocalDateTime lastSentAt;

    @Column(name = "resend_count", nullable = false)
    int resendCount;

    @Lob
    @Column(name = "metadata")
    String metadata;
}
