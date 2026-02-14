package com.acm.auth.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.acm.auth.dto.request.CreateRoleRequest;
import com.acm.auth.dto.request.UpdateRoleRequest;
import com.acm.auth.dto.response.ApiResponse;
import com.acm.auth.dto.response.RoleResponse;
import com.acm.auth.service.RoleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller for Role Management.
 * Only accessible by ADMIN users.
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "CRUD operations for roles (ADMIN only)")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all roles", description = "Retrieve list of all roles in the system")
    public ApiResponse<List<RoleResponse>> getAllRoles() {
        return ApiResponse.success(roleService.getAllRoles());
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get role by code", description = "Retrieve a specific role by its code")
    public ApiResponse<RoleResponse> getRoleByCode(@PathVariable String code) {
        return ApiResponse.success(roleService.getRoleByCode(code));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new role", description = "Create a new role with specified properties")
    public ApiResponse<RoleResponse> createRole(@RequestBody @Valid CreateRoleRequest request) {
        return ApiResponse.success(roleService.createRole(request));
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update role", description = "Update an existing role's properties")
    public ApiResponse<RoleResponse> updateRole(
            @PathVariable String code,
            @RequestBody @Valid UpdateRoleRequest request) {
        return ApiResponse.success(roleService.updateRole(code, request));
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete role", description = "Delete a role (cannot delete ADMIN or USER)")
    public ApiResponse<Void> deleteRole(@PathVariable String code) {
        roleService.deleteRole(code);
        return ApiResponse.success(null);
    }
}
