package org.example.QuanLyMuaVu.Mapper;

import org.example.QuanLyMuaVu.DTO.Request.RoleRequest;
import org.example.QuanLyMuaVu.DTO.Response.RoleResponse;
import org.example.QuanLyMuaVu.Entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "id", ignore = true)
    Role toRole(RoleRequest request);

    @Mapping(source = "id", target = "id")
    RoleResponse toRoleResponse(Role role);
}
