package org.example.QuanLyMuaVu.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserWarningResponse {
    private Long id;
    private Long userId;
    private String decision;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime lockUntil;
    private String userStatus;
}
