package org.example.QuanLyMuaVu.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertSendRequest {
    private String channel;
    private String recipientMode;
    private List<Long> recipientFarmerIds;
}
