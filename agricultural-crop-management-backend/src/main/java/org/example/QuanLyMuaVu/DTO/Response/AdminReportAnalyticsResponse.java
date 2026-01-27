package org.example.QuanLyMuaVu.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Analytics responses for Admin Reports (summary + tab datasets).
 */
@Data
public class AdminReportAnalyticsResponse {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppliedFilters {
        private Integer year;
        private LocalDate dateFrom;
        private LocalDate dateTo;
        private Integer farmId;
        private Integer plotId;
        private Integer cropId;
        private Integer varietyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private BigDecimal actualYield;
        private BigDecimal totalCost;
        private BigDecimal costPerTon;
        private BigDecimal revenue;
        private BigDecimal grossProfit;
        private BigDecimal marginPercent;
        private List<String> warnings;
        private AppliedFilters appliedFilters;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YieldRow {
        private Integer farmId;
        private String farmName;
        private Integer plotId;
        private String plotName;
        private Integer cropId;
        private String cropName;
        private Integer varietyId;
        private String varietyName;
        private BigDecimal actualYield;
        private Long harvestCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YieldTotals {
        private BigDecimal actualYield;
        private Long harvestCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YieldTabResponse {
        private List<YieldRow> tableRows;
        private List<YieldRow> chartSeries;
        private YieldTotals totals;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostCategoryRow {
        private String category;
        private BigDecimal totalCost;
        private Long expenseCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostVendorRow {
        private Integer vendorId;
        private String vendorName;
        private BigDecimal totalCost;
        private Long expenseCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostTimeRow {
        private LocalDate periodStart;
        private String label;
        private BigDecimal totalCost;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostTotals {
        private BigDecimal totalCost;
        private Long expenseCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostTabResponse {
        private List<CostCategoryRow> tableRows;
        private List<CostCategoryRow> chartSeries;
        private CostTotals totals;
        private List<CostVendorRow> vendorRows;
        private List<CostTimeRow> timeSeries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueRow {
        private Integer cropId;
        private String cropName;
        private Integer plotId;
        private String plotName;
        private BigDecimal totalQuantity;
        private BigDecimal totalRevenue;
        private BigDecimal avgPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueTotals {
        private BigDecimal totalQuantity;
        private BigDecimal totalRevenue;
        private BigDecimal avgPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueTabResponse {
        private List<RevenueRow> tableRows;
        private List<RevenueRow> chartSeries;
        private RevenueTotals totals;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfitRow {
        private Integer cropId;
        private String cropName;
        private Integer plotId;
        private String plotName;
        private BigDecimal totalRevenue;
        private BigDecimal totalCost;
        private BigDecimal grossProfit;
        private BigDecimal marginPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfitTotals {
        private BigDecimal totalRevenue;
        private BigDecimal totalCost;
        private BigDecimal grossProfit;
        private BigDecimal marginPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfitTabResponse {
        private List<ProfitRow> tableRows;
        private List<ProfitRow> chartSeries;
        private ProfitTotals totals;
    }
}
