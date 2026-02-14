package com.acm.auth.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new role.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequest {

    @NotBlank(message = "Role code is required")
    @Size(min = 2, max = 50, message = "Role code must be between 2 and 50 characters")
    private String code;

    @NotBlank(message = "Role name is required")
    @Size(min = 2, max = 100, message = "Role name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Min(value = 0, message = "Priority must be at least 0")
    @Max(value = 1000, message = "Priority cannot exceed 1000")
    @Builder.Default
    private Integer priority = 0;

    @Size(max = 100, message = "Redirect path cannot exceed 100 characters")
    @Builder.Default
    private String redirectPath = "/dashboard";
}
