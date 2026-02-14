package com.acm.auth.service;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acm.auth.dto.request.SignUpRequest;
import com.acm.auth.dto.request.SignUpVerifyOtpRequest;
import com.acm.auth.dto.response.OtpChallengeResponse;
import com.acm.auth.dto.response.SignUpVerifyOtpResponse;
import com.acm.auth.dto.response.UserResponse;
import com.acm.auth.entity.Role;
import com.acm.auth.entity.User;
import com.acm.auth.enums.OtpPurpose;
import com.acm.auth.enums.UserStatus;
import com.acm.auth.exception.AppException;
import com.acm.auth.exception.ErrorCode;
import com.acm.auth.repository.RoleRepository;
import com.acm.auth.repository.UserRepository;
import com.acm.auth.service.otp.OtpChallenge;
import com.acm.auth.service.otp.OtpService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    @Transactional
    public OtpChallengeResponse register(SignUpRequest request) {
        String email = request.getEmail();
        String username = request.getUsername();

        var existingByEmail = userRepository.findByEmail(email);
        if (existingByEmail.isPresent()) {
            User existing = existingByEmail.get();
            if (existing.getStatus() == UserStatus.PENDING_VERIFICATION
                    && existing.getUsername().equalsIgnoreCase(username)) {
                return sendOtpChallenge(existing);
            }
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        var existingByUsername = userRepository.findByUsername(username);
        if (existingByUsername.isPresent()) {
            User existing = existingByUsername.get();
            if (existing.getStatus() == UserStatus.PENDING_VERIFICATION
                    && existing.getEmail().equalsIgnoreCase(email)) {
                return sendOtpChallenge(existing);
            }
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        Role role = resolveRole(request.getRole());

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .status(UserStatus.PENDING_VERIFICATION)
                .roles(Set.of(role))
                .joinedDate(LocalDateTime.now())
                .build();

        user = userRepository.save(user);
        log.info("Pending user created: {}", email);

        return sendOtpChallenge(user);
    }

    @Transactional
    public SignUpVerifyOtpResponse verifyOtp(SignUpVerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AppException(ErrorCode.USER_INACTIVE);
        }
        if (user.getStatus() == UserStatus.ACTIVE) {
            return SignUpVerifyOtpResponse.builder()
                    .message("Already verified")
                    .user(toUserResponse(user))
                    .build();
        }

        otpService.verifyOtp(user.getEmail(), OtpPurpose.REGISTER, request.getOtp());

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("User verified and activated: {}", user.getEmail());

        return SignUpVerifyOtpResponse.builder()
                .message("Verified")
                .user(toUserResponse(user))
                .build();
    }

    private OtpChallengeResponse sendOtpChallenge(User user) {
        OtpChallenge challenge = otpService.sendOtp(user.getEmail(), user.getId(), OtpPurpose.REGISTER, true);
        return OtpChallengeResponse.builder()
                .message("OTP sent")
                .nextStep("VERIFY_OTP")
                .emailMasked(challenge.emailMasked())
                .expiresInSeconds(challenge.expiresInSeconds())
                .build();
    }

    private Role resolveRole(String roleCode) {
        String resolvedCode = roleCode != null ? roleCode.toUpperCase() : "USER";
        return roleRepository.findByCode(resolvedCode)
                .orElseGet(() -> roleRepository.findByCode("USER")
                        .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_ERROR, "Default role not found")));
    }

    private UserResponse toUserResponse(User user) {
        String roleCode = user.getRoles().stream().findFirst().map(Role::getCode).orElse(null);
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .status(user.getStatus().name())
                .role(roleCode)
                .build();
    }
}
