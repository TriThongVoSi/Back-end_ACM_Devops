package org.example.QuanLyMuaVu.DTO.Request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
public class FarmUpdateRequest {

    String name;

    Integer provinceId;

    Integer wardId;

    @DecimalMin(value = "0.0", inclusive = false, message = "INVALID_PLOT_AREA")
    BigDecimal area;

    Boolean active;
}
