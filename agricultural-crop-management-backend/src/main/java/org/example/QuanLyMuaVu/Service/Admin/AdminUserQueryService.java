package org.example.QuanLyMuaVu.Service.Admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Entity.Role;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.Repository.UserRepository;
import org.example.QuanLyMuaVu.Repository.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin User Query Service
 * Queries for admin to view and manage users across the system.
 * Uses existing UserRepository and RoleRepository.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminUserQueryService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * DTO for admin user list view
     */
    public record AdminUserDto(
            Long id,
            String username,
            String email,
            String fullName,
            String status,
            List<String> roles) {
    }

    /**
     * Get all users with pagination
     */
    public Page<AdminUserDto> getAllUsers(Pageable pageable) {
        log.info("Admin fetching all users, page: {}", pageable.getPageNumber());

        Page<User> users = userRepository.findAll(pageable);

        return users.map(this::toAdminUserDto);
    }

    /**
     * Search users by keyword (username, email, or fullName)
     */
    public Page<AdminUserDto> searchUsers(String keyword, Pageable pageable) {
        log.info("Admin searching users with keyword: {}", keyword);

        Page<User> users;
        if (keyword != null && !keyword.isBlank()) {
            users = userRepository.searchByKeyword(keyword, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        return users.map(this::toAdminUserDto);
    }

    /**
     * Get all available roles
     */
    public List<String> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(Role::getCode)
                .collect(Collectors.toList());
    }

    /**
     * Count users by status
     */
    public long countByStatus(UserStatus status) {
        return userRepository.countByStatus(status);
    }

    private AdminUserDto toAdminUserDto(User user) {
        List<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getCode).collect(Collectors.toList())
                : List.of();

        return new AdminUserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getStatus() != null ? user.getStatus().name() : null,
                roleNames);
    }
}
