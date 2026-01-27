package org.example.QuanLyMuaVu.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryHealthResponse {
    private LocalDate asOfDate;
    private Integer windowDays;
    private Boolean includeExpiring;
    private Summary summary;
    private List<FarmRisk> farms;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Long expiredLots;
        private Long expiringLots;
        private BigDecimal qtyAtRisk;
        private Long unknownExpiryLots;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FarmRisk {
        private Integer farmId;
        private String farmName;
        private Long expiredLots;
        private Long expiringLots;
        private BigDecimal qtyAtRisk;
        private List<RiskLot> topRiskLots;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskLot {
        private Integer lotId;
        private String itemName;
        private LocalDate expiryDate;
        private BigDecimal onHand;
        private String status;
    }
}
