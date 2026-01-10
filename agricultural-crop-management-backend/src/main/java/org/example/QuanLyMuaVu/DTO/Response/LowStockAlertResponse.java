package org.example.QuanLyMuaVu.DTO.Response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * Low stock alert for dashboard Low Stock Inventory widget
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LowStockAlertResponse {
    Integer supplyLotId;
    String batchCode;
    String itemName;
    String warehouseName;
    String locationLabel;
    BigDecimal onHand;
    String unit;
}
