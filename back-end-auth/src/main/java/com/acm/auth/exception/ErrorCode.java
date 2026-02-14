package com.acm.auth.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {
        // Authentication errors
        INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username/email or password."),
        UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication required."),
        IDENTIFIER_REQUIRED(HttpStatus.BAD_REQUEST, "IDENTIFIER_REQUIRED", "Username or email is required."),

        // Authorization errors
        USER_LOCKED(HttpStatus.FORBIDDEN, "USER_LOCKED", "User account is locked."),
        USER_INACTIVE(HttpStatus.FORBIDDEN, "USER_INACTIVE", "User account is inactive."),
        USER_PENDING_VERIFICATION(HttpStatus.FORBIDDEN, "USER_PENDING_VERIFICATION",
                        "User account is pending verification."),
        ROLE_MISSING(HttpStatus.FORBIDDEN, "ROLE_MISSING", "User has no assigned role."),

        // User errors
        USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."),
        USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", "User already exists."),
        EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Email already registered."),
        USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS", "Username already taken."),

        // Role errors
        ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND", "Role not found."),
        ROLE_ALREADY_EXISTS(HttpStatus.CONFLICT, "ROLE_ALREADY_EXISTS", "Role with this code already exists."),
        CANNOT_DELETE_PREDEFINED_ROLE(HttpStatus.FORBIDDEN, "CANNOT_DELETE_PREDEFINED_ROLE",
                        "Cannot delete predefined roles (ADMIN, USER)."),

        // Validation errors
        INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid request data."),

        // OTP errors
        OTP_INVALID(HttpStatus.BAD_REQUEST, "OTP_INVALID", "Invalid OTP."),
        OTP_EXPIRED(HttpStatus.BAD_REQUEST, "OTP_EXPIRED", "OTP has expired."),
        OTP_TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "OTP_TOO_MANY_ATTEMPTS",
                        "Too many OTP attempts. Please request a new code."),
        OTP_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "OTP_RESEND_TOO_SOON",
                        "OTP was sent recently. Please wait before requesting again."),

        // Password reset token errors
        RESET_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "RESET_TOKEN_INVALID", "Invalid reset token."),
        RESET_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "RESET_TOKEN_EXPIRED", "Reset token has expired."),

        // Google OAuth errors
        GOOGLE_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "GOOGLE_AUTH_FAILED", "Google authentication failed."),

        // Server errors
        INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error.");

        private final HttpStatus httpStatus;
        private final String code;
        private final String message;

        ErrorCode(HttpStatus httpStatus, String code, String message) {
                this.httpStatus = httpStatus;
                this.code = code;
                this.message = message;
        }
}
