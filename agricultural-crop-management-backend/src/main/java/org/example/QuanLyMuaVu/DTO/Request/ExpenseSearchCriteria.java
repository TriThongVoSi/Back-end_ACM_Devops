package org.example.QuanLyMuaVu.DTO.Request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * BR17: Search criteria for expense search functionality.
 * Used by SearchExpense(ExpenseSearchCriteria criteria) method.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseSearchCriteria {

    /**
     * Filter by season ID (optional).
     * If provided, only expenses for this season are returned.
     */
    Integer seasonId;

    /**
     * Filter by plot ID (optional).
     * If provided, only expenses for seasons in this plot are returned.
     */
    Integer plotId;

    /**
     * Filter by crop ID (optional).
     * If provided, only expenses for seasons of this crop are returned.
     */
    Integer cropId;

    /**
     * Filter by task ID (optional).
     * If provided, only expenses linked to this task are returned.
     */
    Integer taskId;

    /**
     * Filter by supplier/vendor ID (optional).
     */
    Integer vendorId;

    /**
     * Filter by category (optional).
     * Matches expenses with the specified category.
     */
    String category;

    /**
     * Filter by payment status (optional).
     */
    PaymentStatus paymentStatus;

    /**
     * Filter by expense date range - start date (optional).
     * Only expenses on or after this date are returned.
     */
    LocalDate fromDate;

    /**
     * Filter by expense date range - end date (optional).
     * Only expenses on or before this date are returned.
     */
    LocalDate toDate;

    /**
     * Filter by minimum amount (optional).
     */
    BigDecimal minAmount;

    /**
     * Filter by maximum amount (optional).
     */
    BigDecimal maxAmount;

    /**
     * Search keyword (optional).
     * Matches item name containing this keyword.
     */
    String keyword;

    /**
     * Alternate search term (optional).
     */
    String q;
}
