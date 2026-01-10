package org.example.QuanLyMuaVu.Mapper;

import org.example.QuanLyMuaVu.DTO.Request.RoleRequest;
import org.example.QuanLyMuaVu.DTO.Response.RoleResponse;
import org.example.QuanLyMuaVu.Entity.Role;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class RoleMapperManual implements RoleMapper {

    @Override
    public Role toRole(RoleRequest request) {
        if (request == null) return null;
        return Role.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    @Override
    public RoleResponse toRoleResponse(Role role) {
        if (role == null) return null;
        return RoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .build();
    }
}
