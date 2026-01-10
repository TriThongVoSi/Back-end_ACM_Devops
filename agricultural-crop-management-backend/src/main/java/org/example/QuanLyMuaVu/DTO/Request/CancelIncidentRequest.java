package org.example.QuanLyMuaVu.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for cancelling an incident (OPEN/IN_PROGRESS -> CANCELLED).
 */
@Data
public class CancelIncidentRequest {

    @NotBlank(message = "Cancellation reason is required")
    private String cancellationReason;
}
