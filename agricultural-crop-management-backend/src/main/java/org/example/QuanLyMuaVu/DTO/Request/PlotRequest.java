package org.example.QuanLyMuaVu.DTO.Request;

import java.math.BigDecimal;

import org.example.QuanLyMuaVu.Enums.PlotStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlotRequest {
    Long userId;
    Integer farmId;

    @NotBlank(message = "Plot name is required")
    String plotName;

    @DecimalMin(value = "0.0", inclusive = false, message = "Area must be greater than 0")
    BigDecimal area;

    String soilType;

    PlotStatus status;
}
