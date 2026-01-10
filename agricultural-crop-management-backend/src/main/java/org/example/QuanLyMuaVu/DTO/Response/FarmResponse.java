package org.example.QuanLyMuaVu.DTO.Response;

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
public class FarmResponse {
    Integer id;
    String farmName;
    String name; // Alias for farmName (for frontend compatibility)
    String ownerUsername; // Owner's username (for admin views)
    Integer provinceId;
    String provinceName;
    Integer wardId;
    String wardName;
    BigDecimal area;
    Boolean active;
}
