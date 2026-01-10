package org.example.QuanLyMuaVu.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

/**
 * Request DTO for updating user status via Admin API.
 */
@Data
@Builder
public class AdminUserStatusUpdateRequest {
    @NotBlank(message = "Status is required")
    private String status;
}
