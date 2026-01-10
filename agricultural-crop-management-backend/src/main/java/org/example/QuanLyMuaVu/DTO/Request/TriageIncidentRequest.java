package org.example.QuanLyMuaVu.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request DTO for triaging an incident (OPEN -> IN_PROGRESS).
 */
@Data
public class TriageIncidentRequest {

    @NotBlank(message = "Severity is required")
    private String severity;

    private LocalDate deadline;

    private Long assigneeId;
}
