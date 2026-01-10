package org.example.QuanLyMuaVu.DTO.Response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Main Dashboard Overview Response DTO
 * Contains all aggregated metrics for the farmer dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardOverviewResponse {

    SeasonContext seasonContext;
    Counts counts;
    Kpis kpis;
    Expenses expenses;
    Harvest harvest;
    Alerts alerts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SeasonContext {
        Integer seasonId;
        String seasonName;
        LocalDate startDate;
        LocalDate endDate;
        LocalDate plannedHarvestDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Counts {
        Integer activeFarms;
        Integer activePlots;
        Map<String, Integer> seasonsByStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Kpis {
        BigDecimal avgYieldTonsPerHa;
        BigDecimal costPerHectare;
        BigDecimal onTimePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Expenses {
        BigDecimal totalExpense;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Harvest {
        BigDecimal totalQuantityKg;
        BigDecimal totalRevenue;
        BigDecimal expectedYieldKg;
        BigDecimal yieldVsPlanPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Alerts {
        Integer openIncidents;
        Integer expiringLots;
        Integer lowStockItems;
    }
}
