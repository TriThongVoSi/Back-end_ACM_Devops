package com.acm.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acm.auth.dto.request.ForgotPasswordRequest;
import com.acm.auth.dto.request.ForgotPasswordVerifyOtpRequest;
import com.acm.auth.dto.request.ResetPasswordRequest;
import com.acm.auth.dto.response.ApiResponse;
import com.acm.auth.dto.response.ForgotPasswordVerifyOtpResponse;
import com.acm.auth.dto.response.OtpChallengeResponse;
import com.acm.auth.dto.response.ResetPasswordResponse;
import com.acm.auth.service.PasswordResetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Password Reset", description = "Forgot password and reset endpoints")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset OTP", description = "Send OTP email if account exists")
    public ApiResponse<OtpChallengeResponse> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        var result = passwordResetService.requestReset(request);
        return ApiResponse.success(result);
    }

    @PostMapping("/forgot-password/verify-otp")
    @Operation(summary = "Verify password reset OTP", description = "Verify OTP and issue temporary reset token")
    public ApiResponse<ForgotPasswordVerifyOtpResponse> verifyForgotPasswordOtp(
            @RequestBody @Valid ForgotPasswordVerifyOtpRequest request) {
        var result = passwordResetService.verifyOtp(request);
        return ApiResponse.success(result);
    }

    @PostMapping("/forgot-password/reset")
    @Operation(summary = "Reset password", description = "Reset password using temporary token")
    public ApiResponse<ResetPasswordResponse> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        var result = passwordResetService.resetPassword(request);
        return ApiResponse.success(result);
    }
}
