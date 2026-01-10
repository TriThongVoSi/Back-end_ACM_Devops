package org.example.QuanLyMuaVu.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class AdminReportResponse {

    // ═══════════════════════════════════════════════════════════════
    // LEGACY DTOs (kept for backward compatibility)
    // ═══════════════════════════════════════════════════════════════

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTotal {
        private Integer year;
        private Integer month;
        private BigDecimal total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeasonHarvest {
        private Integer seasonId;
        private String seasonName;
        private String cropName;
        private BigDecimal totalQuantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidentsSummary {
        private Map<String, Long> bySeverity;
        private Map<String, Long> byStatus;
        private Long totalCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovementSummary {
        private Integer year;
        private Integer month;
        private String movementType;
        private BigDecimal totalQuantity;
    }

    // ═══════════════════════════════════════════════════════════════
    // NEW ANALYTICS DTOs
    // ═══════════════════════════════════════════════════════════════

    /**
     * Yield Report: Compare expected vs actual yield by season/crop/plot.
     * variancePercent = (actualYieldKg - expectedYieldKg) / expectedYieldKg * 100
     * NULL if expectedYieldKg is 0 or null.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YieldReport {
        private Integer seasonId;
        private String seasonName;
        private String cropName;
        private String plotName;
        private String farmName;
        private BigDecimal expectedYieldKg;
        private BigDecimal actualYieldKg;
        private BigDecimal variancePercent; // nullable
    }

    /**
     * Cost Report: Total expenses per season with cost/kg calculation.
     * costPerKg = totalExpense / totalYieldKg
     * NULL if totalYieldKg is 0 (no harvest yet).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostReport {
        private Integer seasonId;
        private String seasonName;
        private String cropName;
        private BigDecimal totalExpense;
        private BigDecimal totalYieldKg;
        private BigDecimal costPerKg; // nullable
    }

    /**
     * Revenue Report: Total revenue from harvests.
     * harvests.unit = price per unit (VND per kg)
     * totalRevenue = SUM(quantity * unit)
     * avgPricePerUnit = totalRevenue / totalQuantity
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueReport {
        private Integer seasonId;
        private String seasonName;
        private String cropName;
        private BigDecimal totalQuantity;
        private BigDecimal totalRevenue;
        private BigDecimal avgPricePerUnit; // nullable if no quantity
    }

    /**
     * Profit Report: Combined revenue and expense analysis.
     * grossProfit = totalRevenue - totalExpense
     * profitMargin = (grossProfit / totalRevenue) * 100, nullable if no revenue
     * returnOnCost = (grossProfit / totalExpense) * 100, nullable if no expense
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfitReport {
        private Integer seasonId;
        private String seasonName;
        private String cropName;
        private String farmName;
        private BigDecimal totalRevenue; // scale 0 (VND)
        private BigDecimal totalExpense; // scale 0 (VND)
        private BigDecimal grossProfit; // scale 0 (VND)
        private BigDecimal profitMargin; // scale 2 (%), nullable if no revenue
        private BigDecimal returnOnCost; // scale 2 (%), nullable if no expense
    }

    /**
     * Task Performance Report: Aggregated task metrics.
     * completionRate = completedTasks / totalTasks * 100
     * overdueRate = overdueTasks / totalTasks * 100
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskPerformanceReport {
        private Long totalTasks;
        private Long completedTasks;
        private Long overdueTasks;
        private Long pendingTasks;
        private Long inProgressTasks;
        private Long cancelledTasks;
        private BigDecimal completionRate; // percentage
        private BigDecimal overdueRate; // percentage
    }

    /**
     * Inventory On-Hand Report: Current stock by warehouse.
     * No price data → just quantity summary.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryOnHandReport {
        private Integer warehouseId;
        private String warehouseName;
        private String farmName;
        private Integer totalLots;
        private BigDecimal totalQuantityOnHand;
        private Integer expiredLots;
        private Integer expiringSoonLots; // expiry within 30 days
    }

    /**
     * Incident Statistics Report: Breakdown by type, severity, status.
     * averageResolutionDays = AVG(DATEDIFF(resolved_at, created_at))
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidentStatisticsReport {
        private Map<String, Long> byIncidentType;
        private Map<String, Long> bySeverity;
        private Map<String, Long> byStatus;
        private Long totalCount;
        private Long openCount;
        private Long resolvedCount;
        private BigDecimal averageResolutionDays; // nullable if no resolved incidents
    }
}
