package com.acm.auth.config;

import java.util.HashSet;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.acm.auth.constant.PredefinedRole;
import com.acm.auth.entity.Role;
import com.acm.auth.entity.User;
import com.acm.auth.enums.UserStatus;
import com.acm.auth.repository.RoleRepository;
import com.acm.auth.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Application initialization configuration.
 * Creates default roles and admin user on startup.
 * 
 * All credentials are configurable via environment variables or
 * application.yml.
 * This makes the auth service reusable across different projects.
 */
@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class ApplicationInitConfig {

    final PasswordEncoder passwordEncoder;

    // Admin account from config (configurable via env variables)
    @Value("${app.init.admin.username:admin}")
    String adminUsername;

    @Value("${app.init.admin.email:admin@acm.local}")
    String adminEmail;

    @Value("${app.init.admin.password:admin123}")
    String adminPassword;

    @Value("${app.init.admin.full-name:Administrator}")
    String adminFullName;

    @Value("${app.init.admin.phone:0900000000}")
    String adminPhone;

    // Flag to control test user creation (disable in production)
    @Value("${app.init.create-test-users:true}")
    boolean createTestUsers;

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository, RoleRepository roleRepository) {
        log.info("Initializing default data (roles and users)...");
        return args -> {
            // 1. Ensure default roles exist (only ADMIN and USER)
            Role adminRole = ensureRoleExists(
                    PredefinedRole.ADMIN_ROLE,
                    "Administrator",
                    "System administrator with full access. Can manage roles and users.",
                    100, // Highest priority
                    "/admin",
                    roleRepository);
            Role userRole = ensureRoleExists(
                    PredefinedRole.USER_ROLE,
                    "User",
                    "Standard user with basic access",
                    10, // Lower priority
                    "/dashboard",
                    roleRepository);

            // 2. Ensure default admin user exists with ADMIN role (always created)
            ensureUserExistsWithRole(adminUsername, adminEmail, adminPassword,
                    adminFullName, adminPhone, adminRole, userRepository);

            // 3. Create test users only if enabled (disable in production)
            if (createTestUsers) {
                log.info("Creating test users (set CREATE_TEST_USERS=false to disable)...");
                ensureUserExistsWithRole("user1", "user1@acm.local", "12345678",
                        "User One", "0901111111", userRole, userRepository);
                ensureUserExistsWithRole("user2", "user2@acm.local", "12345678",
                        "User Two", "0902222222", userRole, userRepository);
            }

            log.info("Default data initialization completed.");
        };
    }

    private Role ensureRoleExists(String code, String name, String description,
            int priority, String redirectPath, RoleRepository roleRepository) {
        return roleRepository.findByCode(code)
                .orElseGet(() -> {
                    log.info("Creating default role with code: {}", code);
                    Role role = Role.builder()
                            .code(code)
                            .name(name)
                            .description(description)
                            .priority(priority)
                            .redirectPath(redirectPath)
                            .build();
                    return roleRepository.save(role);
                });
    }

    private void ensureUserExistsWithRole(String username, String email, String password,
            String fullName, String phone, Role role,
            UserRepository userRepository) {
        Optional<User> existingUserByEmail = userRepository.findByEmail(email);
        Optional<User> existingUserByUsername = userRepository.findByUsername(username);

        if (existingUserByEmail.isPresent()) {
            User user = existingUserByEmail.get();
            boolean needsUpdate = false;
            if (user.getRoles() == null || user.getRoles().isEmpty() ||
                    user.getRoles().stream().noneMatch(r -> r.getCode().equals(role.getCode()))) {
                log.info("Adding missing role {} to user: {}", role.getCode(), email);
                if (user.getRoles() == null) {
                    user.setRoles(new HashSet<>());
                }
                user.getRoles().add(role);
                needsUpdate = true;
            }
            if (needsUpdate) {
                userRepository.save(user);
            } else {
                log.info("User already exists with correct role: {}", email);
            }
            return;
        }

        if (existingUserByUsername.isPresent()) {
            User user = existingUserByUsername.get();
            log.info("User found by username '{}' but with different email. Updating user details.", username);
            user.setEmail(email);
            user.setFullName(fullName);
            user.setPhone(phone);
            user.setPassword(passwordEncoder.encode(password));
            user.setStatus(UserStatus.ACTIVE);

            if (user.getRoles() == null || user.getRoles().isEmpty() ||
                    user.getRoles().stream().noneMatch(r -> r.getCode().equals(role.getCode()))) {
                log.info("Adding missing role {} to user: {}", role.getCode(), email);
                if (user.getRoles() == null) {
                    user.setRoles(new HashSet<>());
                }
                user.getRoles().add(role);
            }
            userRepository.save(user);
            return;
        }

        var roles = new HashSet<Role>();
        roles.add(role);

        User newUser = User.builder()
                .username(username)
                .email(email)
                .fullName(fullName)
                .phone(phone)
                .password(passwordEncoder.encode(password))
                .status(UserStatus.ACTIVE)
                .roles(roles)
                .build();

        userRepository.save(newUser);
        log.info("Default user created: {} / {}", email, password);
    }
}
