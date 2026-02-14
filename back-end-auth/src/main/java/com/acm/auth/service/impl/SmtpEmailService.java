package com.acm.auth.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.acm.auth.enums.OtpPurpose;
import com.acm.auth.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.from-name:Auth Service}")
    private String fromName;

    @Override
    public void sendOtpEmail(String to, String otp, OtpPurpose purpose, long expiresInSeconds) {
        String subject = buildSubject(purpose);
        String body = buildBody(otp, purpose, expiresInSeconds);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(formatFrom());
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        log.info("OTP email sent to {} for purpose {}", to, purpose);
    }

    private String buildSubject(OtpPurpose purpose) {
        return switch (purpose) {
            case REGISTER -> "Your verification code";
            case RESET_PASSWORD -> "Your password reset code";
        };
    }

    private String buildBody(String otp, OtpPurpose purpose, long expiresInSeconds) {
        String minutes = String.valueOf(Math.max(1, expiresInSeconds / 60));
        return switch (purpose) {
            case REGISTER -> """
                    Your verification code is: %s

                    This code expires in %s minutes.
                    If you did not request this, please ignore this email.
                    """.formatted(otp, minutes);
            case RESET_PASSWORD -> """
                    Your password reset code is: %s

                    This code expires in %s minutes.
                    If you did not request this, please ignore this email.
                    """.formatted(otp, minutes);
        };
    }

    private String formatFrom() {
        if (fromName == null || fromName.isBlank()) {
            return from;
        }
        return "%s <%s>".formatted(fromName, from);
    }
}
