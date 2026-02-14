package com.acm.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationRequest {

    /**
     * User identifier - can be email or username
     */
    private String identifier;

    /**
     * Email (alternative to identifier for backward compatibility)
     */
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private boolean rememberMe;

    /**
     * Get effective identifier (prefer identifier, fallback to email)
     */
    public String getEffectiveIdentifier() {
        if (identifier != null && !identifier.isBlank()) {
            return identifier;
        }
        return email;
    }
}
