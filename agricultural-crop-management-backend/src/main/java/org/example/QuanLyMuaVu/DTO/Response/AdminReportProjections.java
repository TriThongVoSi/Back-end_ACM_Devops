package org.example.QuanLyMuaVu.DTO.Response;

import java.math.BigDecimal;

/**
 * Projection interfaces for Admin Reports aggregation queries.
 * Using interfaces instead of Object[] for type-safety.
 */
public class AdminReportProjections {

    /**
     * Expense aggregation by season.
     * Used by: ExpenseRepository.sumExpensesBySeasonIds()
     */
    public interface SeasonExpenseAgg {
        Integer getSeasonId();

        BigDecimal getTotalExpense();
    }

    /**
     * Harvest quantity aggregation by season.
     * Used by: HarvestRepository.sumQuantityBySeasonIds()
     */
    public interface SeasonHarvestAgg {
        Integer getSeasonId();

        BigDecimal getTotalQuantity();
    }

    /**
     * Harvest revenue aggregation by season.
     * Used by: HarvestRepository.sumRevenueBySeasonIds()
     */
    public interface SeasonRevenueAgg {
        Integer getSeasonId();

        BigDecimal getTotalQuantity();

        BigDecimal getTotalRevenue();
    }
}
