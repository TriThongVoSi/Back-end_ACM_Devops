package org.example.QuanLyMuaVu.Mapper;

import org.example.QuanLyMuaVu.DTO.Request.RoleRequest;
import org.example.QuanLyMuaVu.DTO.Request.RoleUpdateRequest;
import org.example.QuanLyMuaVu.DTO.Response.RoleResponse;
import org.example.QuanLyMuaVu.Entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RoleMapper {
    @Mapping(target = "id", ignore = true)
    Role toRole(RoleRequest request);

    @Mapping(source = "id", target = "id")
    RoleResponse toRoleResponse(Role role);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    void updateRole(RoleUpdateRequest request, @MappingTarget Role role);
}
