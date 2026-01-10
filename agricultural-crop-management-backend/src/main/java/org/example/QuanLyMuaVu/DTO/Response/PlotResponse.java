package org.example.QuanLyMuaVu.DTO.Response;

import org.example.QuanLyMuaVu.Enums.PlotStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlotResponse {
    Integer id;
    Integer farmId;
    String farmName;
    String plotName;
    BigDecimal area;
    String soilType;
    PlotStatus status;
}
