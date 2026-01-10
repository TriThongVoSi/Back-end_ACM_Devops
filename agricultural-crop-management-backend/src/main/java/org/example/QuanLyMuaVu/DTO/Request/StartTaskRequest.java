package org.example.QuanLyMuaVu.DTO.Request;

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
public class StartTaskRequest {
    /**
     * Optional actual start date. If not provided, will use current date.
     */
    LocalDate actualStartDate;
}
