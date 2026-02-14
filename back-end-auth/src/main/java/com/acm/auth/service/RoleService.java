package com.acm.auth.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acm.auth.dto.request.CreateRoleRequest;
import com.acm.auth.dto.request.UpdateRoleRequest;
import com.acm.auth.dto.response.RoleResponse;
import com.acm.auth.entity.Role;
import com.acm.auth.exception.AppException;
import com.acm.auth.exception.ErrorCode;
import com.acm.auth.repository.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing roles.
 * Provides CRUD operations for role management by administrators.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;

    /**
     * Get all roles.
     */
    public List<RoleResponse> getAllRoles() {
        log.info("Fetching all roles");
        return roleRepository.findAll().stream()
                .map(this::toRoleResponse)
                .toList();
    }

    /**
     * Get a role by its code.
     */
    public RoleResponse getRoleByCode(String code) {
        log.info("Fetching role by code: {}", code);
        Role role = roleRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        return toRoleResponse(role);
    }

    /**
     * Get a role by its ID.
     */
    public RoleResponse getRoleById(Long id) {
        log.info("Fetching role by id: {}", id);
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        return toRoleResponse(role);
    }

    /**
     * Create a new role.
     */
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        String code = request.getCode().toUpperCase();
        log.info("Creating new role with code: {}", code);

        // Check if role already exists
        if (roleRepository.findByCode(code).isPresent()) {
            log.warn("Role creation failed - code already exists: {}", code);
            throw new AppException(ErrorCode.ROLE_ALREADY_EXISTS);
        }

        Role role = Role.builder()
                .code(code)
                .name(request.getName())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .redirectPath(request.getRedirectPath() != null ? request.getRedirectPath() : "/dashboard")
                .build();

        role = roleRepository.save(role);
        log.info("Role created successfully: {}", code);

        return toRoleResponse(role);
    }

    /**
     * Update an existing role.
     */
    @Transactional
    public RoleResponse updateRole(String code, UpdateRoleRequest request) {
        log.info("Updating role with code: {}", code);

        Role role = roleRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        // Update only provided fields
        if (request.getName() != null) {
            role.setName(request.getName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            role.setPriority(request.getPriority());
        }
        if (request.getRedirectPath() != null) {
            role.setRedirectPath(request.getRedirectPath());
        }

        role = roleRepository.save(role);
        log.info("Role updated successfully: {}", code);

        return toRoleResponse(role);
    }

    /**
     * Delete a role by its code.
     * Note: Cannot delete predefined roles (ADMIN, USER).
     */
    @Transactional
    public void deleteRole(String code) {
        log.info("Deleting role with code: {}", code);

        // Prevent deletion of predefined roles
        if ("ADMIN".equalsIgnoreCase(code) || "USER".equalsIgnoreCase(code)) {
            log.warn("Cannot delete predefined role: {}", code);
            throw new AppException(ErrorCode.CANNOT_DELETE_PREDEFINED_ROLE);
        }

        Role role = roleRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        roleRepository.delete(role);
        log.info("Role deleted successfully: {}", code);
    }

    /**
     * Convert Role entity to RoleResponse DTO.
     */
    private RoleResponse toRoleResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .priority(role.getPriority())
                .redirectPath(role.getRedirectPath())
                .build();
    }
}
