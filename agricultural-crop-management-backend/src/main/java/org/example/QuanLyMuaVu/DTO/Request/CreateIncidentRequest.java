package org.example.QuanLyMuaVu.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateIncidentRequest {

    @NotNull(message = "Season ID is required")
    Integer seasonId;

    @NotBlank(message = "Incident type is required")
    String incidentType;

    @NotBlank(message = "Severity is required")
    String severity;

    @NotBlank(message = "Description is required")
    String description;

    LocalDate deadline;
}
