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

    /**
     * Harvest yield aggregation by farm/plot/crop/variety.
     */
    public interface YieldGroupAgg {
        Integer getFarmId();

        String getFarmName();

        Integer getPlotId();

        String getPlotName();

        Integer getCropId();

        String getCropName();

        Integer getVarietyId();

        String getVarietyName();

        BigDecimal getTotalYield();

        Long getHarvestCount();
    }

    /**
     * Revenue aggregation by crop and plot.
     */
    public interface RevenueGroupAgg {
        Integer getCropId();

        String getCropName();

        Integer getPlotId();

        String getPlotName();

        BigDecimal getTotalQuantity();

        BigDecimal getTotalRevenue();
    }

    /**
     * Cost aggregation by category.
     */
    public interface CostCategoryAgg {
        String getCategory();

        BigDecimal getTotalCost();

        Long getExpenseCount();
    }

    /**
     * Cost aggregation by vendor.
     */
    public interface CostVendorAgg {
        Integer getVendorId();

        String getVendorName();

        BigDecimal getTotalCost();

        Long getExpenseCount();
    }

    /**
     * Cost aggregation by day (base for time series).
     */
    public interface CostTimeAgg {
        java.time.LocalDate getPeriodStart();

        BigDecimal getTotalCost();
    }

    /**
     * Cost aggregation by crop and plot (for profit).
     */
    public interface CostCropPlotAgg {
        Integer getCropId();

        String getCropName();

        Integer getPlotId();

        String getPlotName();

        BigDecimal getTotalCost();
    }
}
