package org.example.QuanLyMuaVu.DTO.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HarvestResponse {
    Integer id;
    Integer seasonId;
    String seasonName;
    LocalDate harvestDate;
    BigDecimal quantity;
    BigDecimal unit;
    String grade;
    BigDecimal revenue;
    String note;
    LocalDateTime createdAt;
}
