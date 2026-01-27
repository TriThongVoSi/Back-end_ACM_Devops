package org.example.QuanLyMuaVu.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * Request DTO for creating a new user via Admin API.
 */
@Data
@Builder
public class AdminUserCreateRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be 8-64 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,64}$", message = "PASSWORD_POLICY_INVALID")
    private String password;

    private String email;
    private String fullName;
    private String phone;
    private Set<String> roles;
}
