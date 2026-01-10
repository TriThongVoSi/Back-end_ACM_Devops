package org.example.QuanLyMuaVu.DTO.Request;

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
public class CompleteTaskRequest {
    /**
     * Required actual end date for the task.
     */
    @NotNull(message = "Actual end date is required")
    LocalDate actualEndDate;

    /**
     * Optional completion notes.
     */
    String completionNotes;
}
