package org.example.QuanLyMuaVu.DTO.Response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * Plot status for dashboard Plot Status Map panel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlotStatusResponse {
    Integer plotId;
    String plotName;
    BigDecimal areaHa;
    String cropName;
    String stage;
    String health;
}
