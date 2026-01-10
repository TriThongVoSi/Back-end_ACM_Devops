package org.example.QuanLyMuaVu.DTO.Response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OnHandRowResponse {

    Integer warehouseId;
    String warehouseName;
    Integer locationId;
    String locationLabel;
    Integer supplyLotId;
    String batchCode;
    String supplyItemName;
    String unit;
    LocalDate expiryDate;
    String lotStatus;
    BigDecimal onHandQuantity;
}
