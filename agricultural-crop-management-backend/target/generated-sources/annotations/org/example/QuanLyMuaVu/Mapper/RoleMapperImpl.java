package org.example.QuanLyMuaVu.Mapper;

import javax.annotation.processing.Generated;
import org.example.QuanLyMuaVu.DTO.Request.RoleRequest;
import org.example.QuanLyMuaVu.DTO.Request.RoleUpdateRequest;
import org.example.QuanLyMuaVu.DTO.Response.RoleResponse;
import org.example.QuanLyMuaVu.Entity.Role;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-27T19:31:04+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.9 (Microsoft)"
)
@Component
public class RoleMapperImpl implements RoleMapper {

    @Override
    public Role toRole(RoleRequest request) {
        if ( request == null ) {
            return null;
        }

        Role.RoleBuilder role = Role.builder();

        role.code( request.getCode() );
        role.name( request.getName() );
        role.description( request.getDescription() );

        return role.build();
    }

    @Override
    public RoleResponse toRoleResponse(Role role) {
        if ( role == null ) {
            return null;
        }

        RoleResponse.RoleResponseBuilder roleResponse = RoleResponse.builder();

        roleResponse.id( role.getId() );
        roleResponse.code( role.getCode() );
        roleResponse.name( role.getName() );
        roleResponse.description( role.getDescription() );

        return roleResponse.build();
    }

    @Override
    public void updateRole(RoleUpdateRequest request, Role role) {
        if ( request == null ) {
            return;
        }

        if ( request.getName() != null ) {
            role.setName( request.getName() );
        }
        if ( request.getDescription() != null ) {
            role.setDescription( request.getDescription() );
        }
    }
}
