package org.example.QuanLyMuaVu.Service.Dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Response.DashboardOverviewResponse;
import org.example.QuanLyMuaVu.Entity.Plot;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Repository.ExpenseRepository;
import org.example.QuanLyMuaVu.Repository.HarvestRepository;
import org.example.QuanLyMuaVu.Repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service responsible for Dashboard KPI calculations.
 * Single Responsibility: Computing performance metrics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardKpiService {

    private final TaskRepository taskRepository;
    private final ExpenseRepository expenseRepository;
    private final HarvestRepository harvestRepository;

    /**
     * Build KPIs for a season.
     */
    public DashboardOverviewResponse.Kpis buildKpis(Season season) {
        if (season == null) {
            return DashboardOverviewResponse.Kpis.builder().build();
        }

        Integer seasonId = season.getId();

        // Cost per hectare
        BigDecimal costPerHectare = calculateCostPerHectare(season, seasonId);

        // On-time percentage
        BigDecimal onTimePercent = calculateOnTimePercent(seasonId);

        // Avg yield: actual_yield_kg / plot_area
        BigDecimal avgYieldTonsPerHa = calculateAvgYieldTonsPerHa(season);

        return DashboardOverviewResponse.Kpis.builder()
                .avgYieldTonsPerHa(avgYieldTonsPerHa)
                .costPerHectare(costPerHectare)
                .onTimePercent(onTimePercent)
                .build();
    }

    /**
     * Build expenses summary for a season.
     */
    public DashboardOverviewResponse.Expenses buildExpenses(Season season) {
        BigDecimal totalExpense = BigDecimal.ZERO;
        if (season != null) {
            totalExpense = expenseRepository.sumTotalCostBySeasonId(season.getId());
        }
        return DashboardOverviewResponse.Expenses.builder()
                .totalExpense(totalExpense)
                .build();
    }

    /**
     * Build harvest summary for a season.
     */
    public DashboardOverviewResponse.Harvest buildHarvest(Season season) {
        if (season == null) {
            return DashboardOverviewResponse.Harvest.builder()
                    .totalQuantityKg(BigDecimal.ZERO)
                    .totalRevenue(BigDecimal.ZERO)
                    .build();
        }

        Integer seasonId = season.getId();
        BigDecimal totalQty = harvestRepository.sumQuantityBySeasonId(seasonId);
        BigDecimal totalRevenue = harvestRepository.sumRevenueBySeasonId(seasonId);
        BigDecimal expectedYieldKg = season.getExpectedYieldKg();

        BigDecimal yieldVsPlanPercent = null;
        if (expectedYieldKg != null && expectedYieldKg.compareTo(BigDecimal.ZERO) > 0) {
            yieldVsPlanPercent = totalQty.subtract(expectedYieldKg)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(expectedYieldKg, 1, RoundingMode.HALF_UP);
        }

        return DashboardOverviewResponse.Harvest.builder()
                .totalQuantityKg(totalQty)
                .totalRevenue(totalRevenue)
                .expectedYieldKg(expectedYieldKg)
                .yieldVsPlanPercent(yieldVsPlanPercent)
                .build();
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private BigDecimal calculateCostPerHectare(Season season, Integer seasonId) {
        BigDecimal totalExpense = expenseRepository.sumTotalCostBySeasonId(seasonId);
        Plot plot = season.getPlot();
        if (plot != null && plot.getArea() != null && plot.getArea().compareTo(BigDecimal.ZERO) > 0) {
            return totalExpense.divide(plot.getArea(), 2, RoundingMode.HALF_UP);
        }
        return null;
    }

    private BigDecimal calculateOnTimePercent(Integer seasonId) {
        long totalCompleted = taskRepository.countCompletedBySeasonId(seasonId);
        if (totalCompleted > 0) {
            long onTime = taskRepository.countCompletedOnTimeBySeasonId(seasonId);
            return BigDecimal.valueOf(onTime)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalCompleted), 1, RoundingMode.HALF_UP);
        }
        return null;
    }

    private BigDecimal calculateAvgYieldTonsPerHa(Season season) {
        Plot plot = season.getPlot();
        if (season.getActualYieldKg() != null && plot != null && plot.getArea() != null
                && plot.getArea().compareTo(BigDecimal.ZERO) > 0) {
            return season.getActualYieldKg()
                    .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP) // kg to tons
                    .divide(plot.getArea(), 2, RoundingMode.HALF_UP);
        }
        return null;
    }
}
