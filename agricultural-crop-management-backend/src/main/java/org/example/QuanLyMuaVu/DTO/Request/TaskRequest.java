package org.example.QuanLyMuaVu.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.TaskStatus;

import java.time.LocalDate;

import static lombok.AccessLevel.PRIVATE;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class TaskRequest {

    Long userId;
    Integer seasonId;
    String title;
    String description;
    LocalDate plannedDate;
    LocalDate dueDate;
    TaskStatus status;
}
