package com.acm.auth.service;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.acm.auth.dto.request.SignUpRequest;
import com.acm.auth.dto.response.UserResponse;
import com.acm.auth.entity.Role;
import com.acm.auth.entity.User;
import com.acm.auth.enums.UserStatus;
import com.acm.auth.exception.AppException;
import com.acm.auth.exception.ErrorCode;
import com.acm.auth.repository.RoleRepository;
import com.acm.auth.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse signUp(SignUpRequest request) {
        log.info("Sign-up attempt for email: {}", request.getEmail());

        // Check existing email
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Sign-up failed - email already exists: {}", request.getEmail());
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Check existing username
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Sign-up failed - username already exists: {}", request.getUsername());
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        // Find role (default to USER)
        String roleCode = request.getRole() != null ? request.getRole().toUpperCase() : "USER";
        Role role = roleRepository.findByCode(roleCode)
                .orElseGet(() -> roleRepository.findByCode("USER")
                        .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_ERROR, "Default role not found")));

        // Create user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .status(UserStatus.ACTIVE)
                .roles(Set.of(role))
                .joinedDate(LocalDateTime.now())
                .build();

        user = userRepository.save(user);
        log.info("User created successfully: {} with role: {}", user.getEmail(), roleCode);

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
