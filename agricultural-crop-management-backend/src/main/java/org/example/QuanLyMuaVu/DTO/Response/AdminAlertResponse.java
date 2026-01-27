package org.example.QuanLyMuaVu.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAlertResponse {
    private Long id;
    private String type;
    private String severity;
    private String status;
    private Integer farmId;
    private String farmName;
    private Integer seasonId;
    private Integer plotId;
    private Integer cropId;
    private String title;
    private String message;
    private String suggestedActionType;
    private String suggestedActionUrl;
    private List<Long> recipientFarmerIds;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}
