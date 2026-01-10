package org.example.QuanLyMuaVu.DTO.Request;

import jakarta.validation.constraints.NotBlank;
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
public class UpdateTaskStatusRequest {

    /**
     * Target status code, e.g. PENDING, IN_PROGRESS, DONE, CANCELLED.
     */
    @NotBlank(message = "KEY_INVALID")
    String status;

    LocalDate actualStartDate;
    LocalDate actualEndDate;
    String notes;
}

