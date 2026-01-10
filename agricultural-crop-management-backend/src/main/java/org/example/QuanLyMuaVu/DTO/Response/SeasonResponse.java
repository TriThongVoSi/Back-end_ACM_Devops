package org.example.QuanLyMuaVu.DTO.Response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeasonResponse {

    Integer id;
    String seasonName;
    Integer plotId;
    Integer cropId;
    Integer varietyId;
    LocalDate startDate;
    LocalDate plannedHarvestDate;
    LocalDate endDate;
    String status;
    BigDecimal expectedYieldKg;
    BigDecimal actualYieldKg;
}
