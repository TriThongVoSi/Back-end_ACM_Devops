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
public class ExpenseRequest {
    Long userId;
    Integer seasonId;
    String itemName;
    BigDecimal unitPrice;
    Integer quantity;
    LocalDate expenseDate;
}
