package org.example.QuanLyMuaVu.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserWarningRequest {
    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Decision is required")
    private String decision;
}
