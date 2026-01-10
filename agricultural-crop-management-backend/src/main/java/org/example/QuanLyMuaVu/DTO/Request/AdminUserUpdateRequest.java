package org.example.QuanLyMuaVu.DTO.Request;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * Request DTO for updating a user via Admin API.
 */
@Data
@Builder
public class AdminUserUpdateRequest {
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private Set<String> roles;
    private String status;
}
