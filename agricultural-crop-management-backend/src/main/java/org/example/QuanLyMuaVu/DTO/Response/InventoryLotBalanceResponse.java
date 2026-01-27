package org.example.QuanLyMuaVu.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLotBalanceResponse {
    private Integer warehouseId;
    private String warehouseName;
    private Integer farmId;
    private String farmName;
    private Integer locationId;
    private String locationLabel;
    private BigDecimal quantity;
}
