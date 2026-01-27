package org.example.QuanLyMuaVu.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLotMovementResponse {
    private Integer movementId;
    private String movementType;
    private BigDecimal quantity;
    private LocalDateTime movementDate;
    private String reference;
    private String note;
}
