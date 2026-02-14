package com.acm.auth.controller;

import java.text.ParseException;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.nimbusds.jose.JOSEException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.acm.auth.dto.request.AuthenticationRequest;
import com.acm.auth.dto.request.GoogleAuthRequest;
import com.acm.auth.dto.request.IntrospectRequest;
import com.acm.auth.dto.request.LogoutRequest;
import com.acm.auth.dto.request.RefreshRequest;
import com.acm.auth.dto.request.SignUpRequest;
import com.acm.auth.dto.request.SignUpVerifyOtpRequest;
import com.acm.auth.dto.response.ApiResponse;
import com.acm.auth.dto.response.AuthenticationResponse;
import com.acm.auth.dto.response.IntrospectResponse;
import com.acm.auth.dto.response.OtpChallengeResponse;
import com.acm.auth.dto.response.SignUpVerifyOtpResponse;
import com.acm.auth.service.AuthenticationService;
import com.acm.auth.service.GoogleAuthService;
import com.acm.auth.service.RegistrationService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final GoogleAuthService googleAuthService;
    private final RegistrationService registrationService;

    @PostMapping("/sign-in")
    @Operation(summary = "Sign in user", description = "Authenticate user by username OR email + password")
    public ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        var result = authenticationService.authenticate(request);
        return ApiResponse.success(result);
    }

    @PostMapping("/google")
    @Operation(summary = "Sign in with Google", description = "Authenticate user using Google ID token")
    public ApiResponse<AuthenticationResponse> googleLogin(@RequestBody @Valid GoogleAuthRequest request) {
        var result = googleAuthService.authenticateWithGoogle(request.getIdToken());
        return ApiResponse.success(result);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user info", description = "Returns current user profile, role, and redirect path")
    public ApiResponse<AuthenticationResponse> getCurrentUser() {
        var result = authenticationService.getCurrentUser();
        return ApiResponse.success(result);
    }

    @PostMapping("/sign-up")
    @Operation(summary = "Register new user", description = "Create new user account with USER role (default). ADMIN can assign other roles.")
    public ApiResponse<OtpChallengeResponse> signUp(@RequestBody @Valid SignUpRequest request) {
        var result = registrationService.register(request);
        return ApiResponse.success(result);
    }

    @PostMapping("/sign-up/verify-otp")
    @Operation(summary = "Verify sign-up OTP", description = "Verify OTP and activate account")
    public ApiResponse<SignUpVerifyOtpResponse> verifySignUpOtp(@RequestBody @Valid SignUpVerifyOtpRequest request) {
        var result = registrationService.verifyOtp(request);
        return ApiResponse.success(result);
    }

    @PostMapping("/introspect")
    @Operation(summary = "Validate token", description = "Check if JWT token is still valid")
    public ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);
        return ApiResponse.success(result);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token", description = "Generate new token when current token is about to expire")
    public ApiResponse<AuthenticationResponse> refreshToken(@RequestBody RefreshRequest request)
            throws ParseException, JOSEException {
        var result = authenticationService.refreshToken(request);
        return ApiResponse.success(result);
    }

    @PostMapping("/sign-out")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Sign out user", description = "Invalidate current JWT token")
    public ApiResponse<Void> logout(@RequestBody LogoutRequest request)
            throws ParseException, JOSEException {
        authenticationService.logout(request);
        return ApiResponse.success(null);
    }
}
