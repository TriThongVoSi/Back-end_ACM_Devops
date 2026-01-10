package org.example.QuanLyMuaVu.Service;

import java.util.List;

import org.springframework.stereotype.Service;

import org.example.QuanLyMuaVu.DTO.Request.RoleRequest;
import org.example.QuanLyMuaVu.DTO.Response.RoleResponse;
import org.example.QuanLyMuaVu.Mapper.RoleMapper;
import org.example.QuanLyMuaVu.Repository.RoleRepository;

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

    public RoleResponse createRole(RoleRequest request) {
        var role = roleMapper.toRole(request);

        role = roleRepository.save(role);
        return roleMapper.toRoleResponse(role);
    }

    public List<RoleResponse> listRoles() {
        return roleRepository.findAll().stream().map(roleMapper::toRoleResponse).toList();
    }

    public void deleteRoleByCode(String roleCode) {
        roleRepository.deleteByCode(roleCode);
    }
}
