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
public class FarmerNotificationResponse {
    private Long id;
    private String title;
    private String message;
    private String link;
    private Long alertId;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
