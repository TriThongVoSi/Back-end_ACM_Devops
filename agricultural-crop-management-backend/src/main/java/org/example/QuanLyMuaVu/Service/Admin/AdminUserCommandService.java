package org.example.QuanLyMuaVu.Service.Admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Request.AdminUserCreateRequest;
import org.example.QuanLyMuaVu.DTO.Request.AdminUserUpdateRequest;
import org.example.QuanLyMuaVu.Entity.Role;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.DocumentFavoriteRepository;
import org.example.QuanLyMuaVu.Repository.DocumentRecentOpenRepository;
import org.example.QuanLyMuaVu.Repository.FarmRepository;
import org.example.QuanLyMuaVu.Repository.PasswordResetTokenRepository;
import org.example.QuanLyMuaVu.Repository.RoleRepository;
import org.example.QuanLyMuaVu.Repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Admin User Command Service.
 * Handles write operations for admin user management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminUserCommandService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FarmRepository farmRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final DocumentFavoriteRepository documentFavoriteRepository;
    private final DocumentRecentOpenRepository documentRecentOpenRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * DTO for user response in admin operations.
     */
    public record AdminUserResponse(
            Long id,
            String username,
            String email,
            String fullName,
            String phone,
            String status,
            List<String> roles) {
    }

    /**
     * Create a new user.
     */
    public AdminUserResponse createUser(AdminUserCreateRequest request) {
        log.info("Admin creating user: {}", request.getUsername());

        // Check username uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        // Check email uniqueness if provided
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setStatus(UserStatus.ACTIVE);
        user.setJoinedDate(LocalDateTime.now());

        // Assign roles
        Set<Role> roles = resolveRoles(request.getRoles());
        user.setRoles(roles);

        user = userRepository.save(user);
        log.info("Successfully created user with ID: {}", user.getId());

        return toResponse(user);
    }

    /**
     * Update an existing user.
     */
    public AdminUserResponse updateUser(Long id, AdminUserUpdateRequest request) {
        log.info("Admin updating user ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Update username if provided and different
        if (request.getUsername() != null && !request.getUsername().isBlank()
                && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
            }
            user.setUsername(request.getUsername());
        }

        // Update email if provided and different
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            user.setEmail(request.getEmail());
        }

        // Update other fields if provided
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        // Update roles if provided
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> roles = resolveRoles(request.getRoles());
            user.setRoles(roles);
        }

        // Update status if provided
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                UserStatus status = UserStatus.valueOf(request.getStatus().toUpperCase());
                user.setStatus(status);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status provided: {}", request.getStatus());
            }
        }

        user = userRepository.save(user);
        log.info("Successfully updated user ID: {}", id);

        return toResponse(user);
    }

    /**
     * Update user status (ACTIVE, LOCKED, INACTIVE).
     */
    public AdminUserResponse updateStatus(Long id, String status) {
        log.info("Admin updating status for user ID: {} to {}", id, status);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        try {
            UserStatus newStatus = UserStatus.valueOf(status.toUpperCase());
            user.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        user = userRepository.save(user);
        log.info("Successfully updated status for user ID: {}", id);

        return toResponse(user);
    }

    /**
     * Reset user password (admin operation).
     */
    public AdminUserResponse resetPassword(Long id, String newPassword) {
        log.info("Admin resetting password for user ID: {}", id);

        if (newPassword == null || newPassword.length() < 8) {
            throw new AppException(ErrorCode.PASSWORD_INVALID);
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(newPassword));
        user = userRepository.save(user);

        log.info("Successfully reset password for user ID: {}", id);
        return toResponse(user);
    }

    /**
     * Get user by ID.
     */
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return toResponse(user);
    }

    /**
     * Check if a user can be deleted (no associated farms).
     */
    @Transactional(readOnly = true)
    public boolean canDelete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return !farmRepository.existsByUserId(id);
    }

    /**
     * Delete a user.
     * Throws exception if user has associated farms.
     * Cleans up related records (password reset tokens, document favorites, recent
     * opens) before deletion.
     */
    public void deleteUser(Long id) {
        log.info("Admin deleting user ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Check for associated farms
        if (farmRepository.existsByUserId(id)) {
            throw new AppException(ErrorCode.USER_HAS_ASSOCIATED_DATA);
        }

        // Delete related records first to avoid foreign key constraints
        log.debug("Deleting related records for user ID: {}", id);

        // Delete password reset tokens
        passwordResetTokenRepository.deleteByUserId(id);

        // Delete document favorites
        documentFavoriteRepository.deleteByUserId(id);

        // Delete document recent opens
        documentRecentOpenRepository.deleteByUserId(id);

        // Clear user roles (to avoid FK constraint on user_roles table)
        user.getRoles().clear();
        userRepository.save(user);

        // Now safely delete the user
        userRepository.delete(user);
        log.info("Successfully deleted user ID: {}", id);
    }

    private Set<Role> resolveRoles(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            // Default to FARMER role
            return roleRepository.findByCode("FARMER")
                    .map(role -> {
                        Set<Role> roles = new HashSet<>();
                        roles.add(role);
                        return roles;
                    })
                    .orElse(new HashSet<>());
        }

        Set<Role> roles = new HashSet<>();
        for (String code : roleCodes) {
            roleRepository.findByCode(code.toUpperCase())
                    .ifPresent(roles::add);
        }
        return roles;
    }

    private AdminUserResponse toResponse(User user) {
        List<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getCode).collect(Collectors.toList())
                : List.of();

        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getStatus() != null ? user.getStatus().name() : null,
                roleNames);
    }
}
