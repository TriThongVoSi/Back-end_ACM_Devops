package org.example.QuanLyMuaVu.DTO.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HarvestSummaryResponse {
    BigDecimal totalHarvestedKg;
    Integer lotsCount;
    BigDecimal totalRevenue;
    BigDecimal yieldVsPlanPercent;
    BigDecimal expectedYieldKg;
    BigDecimal actualYieldKg;
}
