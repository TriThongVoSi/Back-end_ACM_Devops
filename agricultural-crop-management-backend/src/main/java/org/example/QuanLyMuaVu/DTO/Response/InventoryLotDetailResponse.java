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
public class InventoryLotDetailResponse {
    private Integer lotId;
    private Integer itemId;
    private String itemName;
    private String lotCode;
    private String unit;
    private String supplierName;
    private LocalDate expiryDate;
    private String status;
    private BigDecimal onHandTotal;
    private List<InventoryLotBalanceResponse> balances;
}
