package com.acm.auth.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing role.
 * All fields are optional - only provided fields will be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequest {

    @Size(min = 2, max = 100, message = "Role name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Min(value = 0, message = "Priority must be at least 0")
    @Max(value = 1000, message = "Priority cannot exceed 1000")
    private Integer priority;

    @Size(max = 100, message = "Redirect path cannot exceed 100 characters")
    private String redirectPath;
}
