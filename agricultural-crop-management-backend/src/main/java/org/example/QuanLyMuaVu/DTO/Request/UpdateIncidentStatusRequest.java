package org.example.QuanLyMuaVu.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateIncidentStatusRequest {
    @NotBlank(message = "Status is required")
    private String status;
}
