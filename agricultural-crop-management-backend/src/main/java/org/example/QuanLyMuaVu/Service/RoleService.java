package org.example.QuanLyMuaVu.Service;

import java.util.List;

import org.example.QuanLyMuaVu.DTO.Request.RoleRequest;
import org.example.QuanLyMuaVu.DTO.Request.RoleUpdateRequest;
import org.example.QuanLyMuaVu.DTO.Response.RoleResponse;
import org.example.QuanLyMuaVu.Entity.Role;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Mapper.RoleMapper;
import org.example.QuanLyMuaVu.Repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleService {
    RoleRepository roleRepository;
    RoleMapper roleMapper;

    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        // Check for duplicate code
        if (roleRepository.findByCode(request.getCode()).isPresent()) {
            throw new AppException(ErrorCode.ROLE_CODE_EXISTS);
        }
        var role = roleMapper.toRole(request);
        role = roleRepository.save(role);
        log.info("Created new role: {}", role.getCode());
        return roleMapper.toRoleResponse(role);
    }

    public List<RoleResponse> listRoles() {
        return roleRepository.findAll().stream().map(roleMapper::toRoleResponse).toList();
    }

    public RoleResponse getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        return roleMapper.toRoleResponse(role);
    }

    @Transactional
    public RoleResponse updateRole(Long id, RoleUpdateRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        roleMapper.updateRole(request, role);
        role = roleRepository.save(role);
        log.info("Updated role: {}", role.getCode());
        return roleMapper.toRoleResponse(role);
    }

    @Transactional
    public void deleteRoleByCode(String roleCode) {
        if (roleRepository.findByCode(roleCode).isEmpty()) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        }
        roleRepository.deleteByCode(roleCode);
        log.info("Deleted role: {}", roleCode);
    }
}
