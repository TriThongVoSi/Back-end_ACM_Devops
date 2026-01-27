package org.example.QuanLyMuaVu.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryRiskLotResponse {
    private Integer lotId;
    private Integer itemId;
    private String itemName;
    private String lotCode;
    private Integer farmId;
    private String farmName;
    private LocalDate expiryDate;
    private BigDecimal onHand;
    private Long daysToExpiry;
    private String status;
    private String unit;
    private BigDecimal unitCost;
}
