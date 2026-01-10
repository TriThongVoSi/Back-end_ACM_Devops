package org.example.QuanLyMuaVu.Service.Admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Request.AdminReportFilter;
import org.example.QuanLyMuaVu.DTO.Response.AdminReportProjections;
import org.example.QuanLyMuaVu.DTO.Response.AdminReportResponse;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Repository.ExpenseRepository;
import org.example.QuanLyMuaVu.Repository.HarvestRepository;
import org.example.QuanLyMuaVu.Repository.SeasonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admin Report Service
 * Generates financial and operational reports for admin.
 * Uses existing repositories with aggregation queries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminReportService {

        private final ExpenseRepository expenseRepository;
        private final HarvestRepository harvestRepository;
        private final SeasonRepository seasonRepository;

        /**
         * Get yield report - total harvest quantities by season with variance
         * calculation.
         * variancePercent = (actualYieldKg - expectedYieldKg) / expectedYieldKg * 100
         */
        public List<AdminReportResponse.YieldReport> getYieldReport(AdminReportFilter filter) {
                log.info("Generating yield report with filter: {}", filter);

                // 1. Get seasons matching filters
                List<Season> seasons = seasonRepository.findByFilters(
                                filter.getEffectiveFromDate(),
                                filter.getEffectiveToDate(),
                                filter.getCropId(),
                                filter.getFarmId(),
                                filter.getPlotId());

                if (seasons.isEmpty()) {
                        return Collections.emptyList();
                }

                // 2. Collect season IDs and fetch harvest aggregates
                Set<Integer> seasonIds = seasons.stream()
                                .map(Season::getId)
                                .collect(Collectors.toSet());

                Map<Integer, BigDecimal> harvestMap = harvestRepository.sumQuantityBySeasonIds(seasonIds)
                                .stream()
                                .collect(Collectors.toMap(
                                                AdminReportProjections.SeasonHarvestAgg::getSeasonId,
                                                AdminReportProjections.SeasonHarvestAgg::getTotalQuantity));

                // 3. Build response DTOs with variance calculation
                return seasons.stream()
                                .map(season -> {
                                        BigDecimal expected = season.getExpectedYieldKg() != null
                                                        ? season.getExpectedYieldKg()
                                                        : BigDecimal.ZERO;
                                        BigDecimal actual = harvestMap.getOrDefault(season.getId(), BigDecimal.ZERO);
                                        BigDecimal variance = calculateVariancePercent(expected, actual);

                                        return AdminReportResponse.YieldReport.builder()
                                                        .seasonId(season.getId())
                                                        .seasonName(season.getSeasonName())
                                                        .cropName(season.getCrop() != null
                                                                        ? season.getCrop().getCropName()
                                                                        : null)
                                                        .plotName(season.getPlot() != null
                                                                        ? season.getPlot().getPlotName()
                                                                        : null)
                                                        .farmName(season.getPlot() != null
                                                                        && season.getPlot().getFarm() != null
                                                                                        ? season.getPlot().getFarm()
                                                                                                        .getName()
                                                                                        : null)
                                                        .expectedYieldKg(expected)
                                                        .actualYieldKg(actual)
                                                        .variancePercent(variance)
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        /**
         * Get cost report - total expenses by season with cost per kg calculation.
         * costPerKg = totalExpense / totalYieldKg
         */
        public List<AdminReportResponse.CostReport> getCostReport(AdminReportFilter filter) {
                log.info("Generating cost report with filter: {}", filter);

                // 1. Get seasons matching filters
                List<Season> seasons = seasonRepository.findByFilters(
                                filter.getEffectiveFromDate(),
                                filter.getEffectiveToDate(),
                                filter.getCropId(),
                                filter.getFarmId(),
                                filter.getPlotId());

                if (seasons.isEmpty()) {
                        return Collections.emptyList();
                }

                // 2. Collect season IDs and fetch aggregates
                Set<Integer> seasonIds = seasons.stream()
                                .map(Season::getId)
                                .collect(Collectors.toSet());

                Map<Integer, BigDecimal> expenseMap = expenseRepository.sumExpensesBySeasonIds(seasonIds)
                                .stream()
                                .collect(Collectors.toMap(
                                                AdminReportProjections.SeasonExpenseAgg::getSeasonId,
                                                AdminReportProjections.SeasonExpenseAgg::getTotalExpense));

                Map<Integer, BigDecimal> harvestMap = harvestRepository.sumQuantityBySeasonIds(seasonIds)
                                .stream()
                                .collect(Collectors.toMap(
                                                AdminReportProjections.SeasonHarvestAgg::getSeasonId,
                                                AdminReportProjections.SeasonHarvestAgg::getTotalQuantity));

                // 3. Build response DTOs with cost/kg calculation
                return seasons.stream()
                                .map(season -> {
                                        BigDecimal expense = expenseMap.getOrDefault(season.getId(), BigDecimal.ZERO);
                                        BigDecimal yield = harvestMap.getOrDefault(season.getId(), BigDecimal.ZERO);
                                        BigDecimal costPerKg = calculateCostPerKg(expense, yield);

                                        return AdminReportResponse.CostReport.builder()
                                                        .seasonId(season.getId())
                                                        .seasonName(season.getSeasonName())
                                                        .cropName(season.getCrop() != null
                                                                        ? season.getCrop().getCropName()
                                                                        : null)
                                                        .totalExpense(expense)
                                                        .totalYieldKg(yield)
                                                        .costPerKg(costPerKg)
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        /**
         * Get revenue report - total revenue with average price calculation.
         * avgPricePerUnit = totalRevenue / totalQuantity
         */
        public List<AdminReportResponse.RevenueReport> getRevenueReport(AdminReportFilter filter) {
                log.info("Generating revenue report with filter: {}", filter);

                // 1. Get seasons matching filters
                List<Season> seasons = seasonRepository.findByFilters(
                                filter.getEffectiveFromDate(),
                                filter.getEffectiveToDate(),
                                filter.getCropId(),
                                filter.getFarmId(),
                                filter.getPlotId());

                if (seasons.isEmpty()) {
                        return Collections.emptyList();
                }

                // 2. Collect season IDs and fetch revenue aggregates
                Set<Integer> seasonIds = seasons.stream()
                                .map(Season::getId)
                                .collect(Collectors.toSet());

                Map<Integer, AdminReportProjections.SeasonRevenueAgg> revenueMap = harvestRepository
                                .sumRevenueBySeasonIds(seasonIds)
                                .stream()
                                .collect(Collectors.toMap(
                                                AdminReportProjections.SeasonRevenueAgg::getSeasonId,
                                                Function.identity()));

                // 3. Build response DTOs with avg price calculation
                return seasons.stream()
                                .map(season -> {
                                        AdminReportProjections.SeasonRevenueAgg agg = revenueMap.get(season.getId());
                                        BigDecimal quantity = agg != null ? agg.getTotalQuantity() : BigDecimal.ZERO;
                                        BigDecimal revenue = agg != null ? agg.getTotalRevenue() : BigDecimal.ZERO;
                                        BigDecimal avgPrice = calculateAvgPrice(revenue, quantity);

                                        return AdminReportResponse.RevenueReport.builder()
                                                        .seasonId(season.getId())
                                                        .seasonName(season.getSeasonName())
                                                        .cropName(season.getCrop() != null
                                                                        ? season.getCrop().getCropName()
                                                                        : null)
                                                        .totalQuantity(quantity)
                                                        .totalRevenue(revenue)
                                                        .avgPricePerUnit(avgPrice)
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        /**
         * Get profit report - combined revenue and expense analysis.
         * grossProfit = totalRevenue - totalExpense
         * profitMargin = (grossProfit / totalRevenue) * 100
         * returnOnCost = (grossProfit / totalExpense) * 100
         */
        public List<AdminReportResponse.ProfitReport> getProfitReport(AdminReportFilter filter) {
                log.info("Generating profit report with filter: {}", filter);

                // 1. Get seasons matching filters
                List<Season> seasons = seasonRepository.findByFilters(
                                filter.getEffectiveFromDate(),
                                filter.getEffectiveToDate(),
                                filter.getCropId(),
                                filter.getFarmId(),
                                filter.getPlotId());

                if (seasons.isEmpty()) {
                        return Collections.emptyList();
                }

                // 2. Collect season IDs and fetch aggregates
                Set<Integer> seasonIds = seasons.stream()
                                .map(Season::getId)
                                .collect(Collectors.toSet());

                Map<Integer, BigDecimal> revenueMap = harvestRepository.sumRevenueBySeasonIds(seasonIds)
                                .stream()
                                .collect(Collectors.toMap(
                                                AdminReportProjections.SeasonRevenueAgg::getSeasonId,
                                                AdminReportProjections.SeasonRevenueAgg::getTotalRevenue));

                Map<Integer, BigDecimal> expenseMap = expenseRepository.sumExpensesBySeasonIds(seasonIds)
                                .stream()
                                .collect(Collectors.toMap(
                                                AdminReportProjections.SeasonExpenseAgg::getSeasonId,
                                                AdminReportProjections.SeasonExpenseAgg::getTotalExpense));

                // 3. Build response DTOs with profit calculations
                return seasons.stream()
                                .map(season -> {
                                        BigDecimal revenue = revenueMap.getOrDefault(season.getId(), BigDecimal.ZERO);
                                        BigDecimal expense = expenseMap.getOrDefault(season.getId(), BigDecimal.ZERO);
                                        BigDecimal profit = revenue.subtract(expense);
                                        BigDecimal margin = calculatePercentage(profit, revenue);
                                        BigDecimal roc = calculatePercentage(profit, expense);

                                        return AdminReportResponse.ProfitReport.builder()
                                                        .seasonId(season.getId())
                                                        .seasonName(season.getSeasonName())
                                                        .cropName(season.getCrop() != null
                                                                        ? season.getCrop().getCropName()
                                                                        : null)
                                                        .farmName(season.getPlot() != null
                                                                        && season.getPlot().getFarm() != null
                                                                                        ? season.getPlot().getFarm()
                                                                                                        .getName()
                                                                                        : null)
                                                        .totalRevenue(revenue)
                                                        .totalExpense(expense)
                                                        .grossProfit(profit)
                                                        .profitMargin(margin)
                                                        .returnOnCost(roc)
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        /**
         * Get simple date-range summary for dashboard
         */
        public ReportSummary getSummary(LocalDate startDate, LocalDate endDate) {
                if (startDate == null) {
                        startDate = LocalDate.now().withDayOfYear(1);
                }
                if (endDate == null) {
                        endDate = LocalDate.now();
                }

                BigDecimal totalExpenses = expenseRepository.sumAmountByExpenseDateBetween(startDate, endDate);
                BigDecimal totalHarvest = harvestRepository.sumQuantityByHarvestDateBetween(startDate, endDate);

                return new ReportSummary(
                                startDate,
                                endDate,
                                totalExpenses != null ? totalExpenses : BigDecimal.ZERO,
                                totalHarvest != null ? totalHarvest : BigDecimal.ZERO);
        }

        // ═══════════════════════════════════════════════════════════════
        // CALCULATION HELPERS
        // ═══════════════════════════════════════════════════════════════

        /**
         * Calculate variance percentage: (actual - expected) / expected * 100
         * Returns null if expected is zero or null.
         */
        private BigDecimal calculateVariancePercent(BigDecimal expected, BigDecimal actual) {
                if (expected == null || expected.compareTo(BigDecimal.ZERO) == 0) {
                        return null;
                }
                return actual.subtract(expected)
                                .multiply(BigDecimal.valueOf(100))
                                .divide(expected, 2, RoundingMode.HALF_UP);
        }

        /**
         * Calculate cost per kg: expense / yield
         * Returns null if yield is zero.
         */
        private BigDecimal calculateCostPerKg(BigDecimal expense, BigDecimal yield) {
                if (yield == null || yield.compareTo(BigDecimal.ZERO) == 0) {
                        return null;
                }
                return expense.divide(yield, 2, RoundingMode.HALF_UP);
        }

        /**
         * Calculate average price: revenue / quantity
         * Returns null if quantity is zero.
         */
        private BigDecimal calculateAvgPrice(BigDecimal revenue, BigDecimal quantity) {
                if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
                        return null;
                }
                return revenue.divide(quantity, 2, RoundingMode.HALF_UP);
        }

        /**
         * Calculate percentage: numerator / denominator * 100
         * Returns null if denominator is zero.
         */
        private BigDecimal calculatePercentage(BigDecimal numerator, BigDecimal denominator) {
                if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
                        return null;
                }
                return numerator.multiply(BigDecimal.valueOf(100))
                                .divide(denominator, 2, RoundingMode.HALF_UP);
        }

        public record ReportSummary(
                        LocalDate startDate,
                        LocalDate endDate,
                        BigDecimal totalExpenses,
                        BigDecimal totalHarvest) {
        }
}
