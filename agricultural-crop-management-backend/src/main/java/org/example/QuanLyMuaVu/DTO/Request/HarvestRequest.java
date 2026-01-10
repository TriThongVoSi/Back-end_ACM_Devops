package org.example.QuanLyMuaVu.DTO.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HarvestRequest {
    Integer seasonId;
    LocalDate harvestDate;
    BigDecimal quantity;
    BigDecimal unit;
    String grade;
    String note;
}
