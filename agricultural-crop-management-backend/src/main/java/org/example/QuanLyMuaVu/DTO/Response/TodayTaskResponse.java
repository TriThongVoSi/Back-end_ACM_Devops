package org.example.QuanLyMuaVu.DTO.Response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

/**
 * Task summary for dashboard Today's Tasks table
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TodayTaskResponse {
    Integer taskId;
    String title;
    String plotName;
    String type;
    String assigneeName;
    LocalDate dueDate;
    String status;
}
