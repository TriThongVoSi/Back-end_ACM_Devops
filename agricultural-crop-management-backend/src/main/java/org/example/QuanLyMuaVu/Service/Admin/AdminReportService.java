package org.example.QuanLyMuaVu.Service.Admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Request.AdminReportFilter;
import org.example.QuanLyMuaVu.DTO.Response.AdminReportAnalyticsResponse;
import org.example.QuanLyMuaVu.DTO.Response.AdminReportOptionsResponse;
import org.example.QuanLyMuaVu.DTO.Response.AdminReportProjections;
import org.example.QuanLyMuaVu.DTO.Response.AdminReportResponse;
import org.example.QuanLyMuaVu.Entity.Crop;
import org.example.QuanLyMuaVu.Entity.Farm;
import org.example.QuanLyMuaVu.Entity.Plot;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Entity.Variety;
import org.example.QuanLyMuaVu.Enums.AdminReportTab;
import org.example.QuanLyMuaVu.Enums.ExpenseTimeGranularity;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.CropRepository;
import org.example.QuanLyMuaVu.Repository.ExpenseRepository;
import org.example.QuanLyMuaVu.Repository.FarmRepository;
import org.example.QuanLyMuaVu.Repository.HarvestRepository;
import org.example.QuanLyMuaVu.Repository.PlotRepository;
import org.example.QuanLyMuaVu.Repository.SeasonRepository;
import org.example.QuanLyMuaVu.Repository.VarietyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
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
        private final FarmRepository farmRepository;
        private final CropRepository cropRepository;
        private final PlotRepository plotRepository;
        private final VarietyRepository varietyRepository;

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
                                filter.getPlotId(),
                                filter.getVarietyId());

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
                                filter.getPlotId(),
                                filter.getVarietyId());

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
                                filter.getPlotId(),
                                filter.getVarietyId());

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
                                filter.getPlotId(),
                                filter.getVarietyId());

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
         * Analytics summary for admin reports (KPIs).
         */
        public AdminReportAnalyticsResponse.Summary getSummary(AdminReportFilter filter) {
                log.info("Generating admin summary with filter: {}", filter);
                validateFilters(filter);

                LocalDate fromDate = filter.getEffectiveFromDate();
                LocalDate toDate = filter.getEffectiveToDateInclusive();

                BigDecimal totalCost = zeroIfNull(expenseRepository.sumAmountByFilters(
                                fromDate,
                                toDate,
                                filter.getFarmId(),
                                filter.getPlotId(),
                                filter.getCropId(),
                                filter.getVarietyId()));
                BigDecimal actualYield = zeroIfNull(harvestRepository.sumQuantityByFilters(
                                fromDate,
                                toDate,
                                filter.getFarmId(),
                                filter.getPlotId(),
                                filter.getCropId(),
                                filter.getVarietyId()));
                BigDecimal revenue = zeroIfNull(harvestRepository.sumRevenueByFilters(
                                fromDate,
                                toDate,
                                filter.getFarmId(),
                                filter.getPlotId(),
                                filter.getCropId(),
                                filter.getVarietyId()));

                BigDecimal costPerTon = calculateCostPerKg(totalCost, actualYield);
                BigDecimal grossProfit = revenue.subtract(totalCost);
                BigDecimal marginPercent = calculatePercentage(grossProfit, revenue);

                List<String> warnings = new ArrayList<>();
                long missingPriceCount = harvestRepository.countMissingUnitPriceByFilters(
                                fromDate,
                                toDate,
                                filter.getFarmId(),
                                filter.getPlotId(),
                                filter.getCropId(),
                                filter.getVarietyId());
                if (missingPriceCount > 0) {
                        warnings.add("Missing unit price data for " + missingPriceCount
                                        + " harvests. Revenue and profit may be understated.");
                }

                return AdminReportAnalyticsResponse.Summary.builder()
                                .actualYield(actualYield)
                                .totalCost(totalCost)
                                .costPerTon(costPerTon)
                                .revenue(revenue)
                                .grossProfit(grossProfit)
                                .marginPercent(marginPercent)
                                .warnings(warnings.isEmpty() ? null : warnings)
                                .appliedFilters(buildAppliedFilters(filter))
                                .build();
        }

        /**
         * Yield tab analytics: breakdown by farm -> plot -> crop -> variety.
         */
        public AdminReportAnalyticsResponse.YieldTabResponse getYieldAnalytics(AdminReportFilter filter) {
                log.info("Generating yield analytics with filter: {}", filter);
                validateFilters(filter);

                LocalDate fromDate = filter.getEffectiveFromDate();
                LocalDate toDate = filter.getEffectiveToDateInclusive();

                List<AdminReportAnalyticsResponse.YieldRow> rows = harvestRepository.sumYieldByGroup(
                                fromDate,
                                toDate,
                                filter.getFarmId(),
                                filter.getPlotId(),
                                filter.getCropId(),
                                filter.getVarietyId())
                                .stream()
                                .map(agg -> AdminReportAnalyticsResponse.YieldRow.builder()
                                                .farmId(agg.getFarmId())
                                                .farmName(agg.getFarmName())
                                                .plotId(agg.getPlotId())
                                                .plotName(agg.getPlotName())
                                                .cropId(agg.getCropId())
                                                .cropName(agg.getCropName())
                                                .varietyId(agg.getVarietyId())
                                                .varietyName(agg.getVarietyName())
                                                .actualYield(zeroIfNull(agg.getTotalYield()))
                                                .harvestCount(agg.getHarvestCount() != null ? agg.getHarvestCount() : 0L)
                                                .build())
                                .sorted(Comparator.comparing(AdminReportAnalyticsResponse.YieldRow::getActualYield)
                                                .reversed())
                                .toList();

                BigDecimal totalYield = rows.stream()
                                .map(AdminReportAnalyticsResponse.YieldRow::getActualYield)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                long totalHarvestCount = rows.stream()
                                .map(AdminReportAnalyticsResponse.YieldRow::getHarvestCount)
                                .filter(Objects::nonNull)
                                .mapToLong(Long::longValue)
                                .sum();

                return AdminReportAnalyticsResponse.YieldTabResponse.builder()
                                .tableRows(rows)
                                .chartSeries(rows)
                                .totals(AdminReportAnalyticsResponse.YieldTotals.builder()
                                                .actualYield(totalYield)
                                                .harvestCount(totalHarvestCount)
                                                .build())
                                .build();
        }

        /**
         * Cost tab analytics: category, vendor, and time series breakdowns.
         */
        public AdminReportAnalyticsResponse.CostTabResponse getCostAnalytics(
                        AdminReportFilter filter,
                        ExpenseTimeGranularity granularity) {
                log.info("Generating cost analytics with filter: {}", filter);
                validateFilters(filter);

                LocalDate fromDate = filter.getEffectiveFromDate();
                LocalDate toDate = filter.getEffectiveToDateInclusive();

                BigDecimal totalCost = zeroIfNull(expenseRepository.sumAmountByFilters(
                                fromDate,
                                toDate,
                                filter.getFarmId(),
                                filter.getPlotId(),
                                filter.getCropId(),
                                filter.getVarietyId()));
                long expenseCount = expenseRepository.countByFilters(
                                fromDate,
                                toDate,
                                filter.getFarmId(),
                                filter.getPlotId(),
                                filter.getCropId(),
                                filter.getVarietyId());

                Map<String, AdminReportAnalyticsResponse.CostCategoryRow> categoryMap = new LinkedHashMap<>();
                for (AdminReportProjections.CostCategoryAgg agg : expenseRepository.sumCostByCategory(
                                fromDate,
                                toDate,
                                filter.getFarmId(),
                                filter.getPlotId(),
                                filter.getCropId(),
                                filter.getVarietyId())) {
                        String category = normalizeCategory(agg.getCategory());
                        BigDecimal amount = zeroIfNull(agg.getTotalCost());
                        long count = agg.getExpenseCount() != null ? agg.getExpenseCount() : 0L;
                        AdminReportAnalyticsResponse.CostCategoryRow existing = categoryMap.get(category);
                        if (existing == null) {
                                categoryMap.put(category, AdminReportAnalyticsResponse.CostCategoryRow.builder()
                                                .category(category)
                                                .totalCost(amount)
                                                .expenseCount(count)
                                                .build());
                        } else {
                                existing.setTotalCost(existing.getTotalCost().add(amount));
                                existing.setExpenseCount(existing.getExpenseCount() + count);
                        }
                }

                List<AdminReportAnalyticsResponse.CostCategoryRow> categoryRows = new ArrayList<>(categoryMap.values());
                categoryRows.sort(Comparator.comparing(AdminReportAnalyticsResponse.CostCategoryRow::getTotalCost)
                                .reversed());

                Map<String, AdminReportAnalyticsResponse.CostVendorRow> vendorMap = new LinkedHashMap<>();
                for (AdminReportProjections.CostVendorAgg agg : expenseRepository.sumCostByVendor(
                                fromDate,
                                toDate,
                                filter.getFarmId(),
                                filter.getPlotId(),
                                filter.getCropId(),
                                filter.getVarietyId())) {
                        String vendorName = normalizeVendor(agg.getVendorName());
                        Integer vendorId = agg.getVendorId();
                        BigDecimal amount = zeroIfNull(agg.getTotalCost());
                        long count = agg.getExpenseCount() != null ? agg.getExpenseCount() : 0L;
                        String key = vendorId != null ? vendorId.toString() : "unassigned";
                        AdminReportAnalyticsResponse.CostVendorRow existing = vendorMap.get(key);
                        if (existing == null) {
                                vendorMap.put(key, AdminReportAnalyticsResponse.CostVendorRow.builder()
                                                .vendorId(vendorId)
                                                .vendorName(vendorName)
                                                .totalCost(amount)
                                                .expenseCount(count)
                                                .build());
                        } else {
                                existing.setTotalCost(existing.getTotalCost().add(amount));
                                existing.setExpenseCount(existing.getExpenseCount() + count);
                        }
                }

                List<AdminReportAnalyticsResponse.CostVendorRow> vendorRows = new ArrayList<>(vendorMap.values());
                vendorRows.sort(Comparator.comparing(AdminReportAnalyticsResponse.CostVendorRow::getTotalCost)
                                .reversed());

                List<AdminReportAnalyticsResponse.CostTimeRow> timeSeries = buildTimeSeries(
                                expenseRepository.sumCostByDay(
                                                fromDate,
                                                toDate,
                                                filter.getFarmId(),
                                                filter.getPlotId(),
                                                filter.getCropId(),
                                                filter.getVarietyId()),
                                granularity != null ? granularity : ExpenseTimeGranularity.MONTH,
                                fromDate,
                                toDate);

                return AdminReportAnalyticsResponse.CostTabResponse.builder()
                                .tableRows(categoryRows)
                                .chartSeries(categoryRows)
                                .totals(AdminReportAnalyticsResponse.CostTotals.builder()
                                                .totalCost(totalCost)
                                                .expenseCount(expenseCount)
                                                .build())
                                .vendorRows(vendorRows)
                                .timeSeries(timeSeries)
                                .build();
        }

        /**
         * Revenue tab analytics: revenue by crop and plot.
         */
        public AdminReportAnalyticsResponse.RevenueTabResponse getRevenueAnalytics(AdminReportFilter filter) {
                log.info("Generating revenue analytics with filter: {}", filter);
                validateFilters(filter);

                LocalDate fromDate = filter.getEffectiveFromDate();
                LocalDate toDate = filter.getEffectiveToDateInclusive();

                List<AdminReportAnalyticsResponse.RevenueRow> rows = harvestRepository.sumRevenueByCropPlot(
                                fromDate,
                                toDate,
                                filter.getFarmId(),
                                filter.getPlotId(),
                                filter.getCropId(),
                                filter.getVarietyId())
                                .stream()
                                .map(agg -> {
                                        BigDecimal quantity = zeroIfNull(agg.getTotalQuantity());
                                        BigDecimal revenue = zeroIfNull(agg.getTotalRevenue());
                                        return AdminReportAnalyticsResponse.RevenueRow.builder()
                                                        .cropId(agg.getCropId())
                                                        .cropName(agg.getCropName())
                                                        .plotId(agg.getPlotId())
                                                        .plotName(agg.getPlotName())
                                                        .totalQuantity(quantity)
                                                        .totalRevenue(revenue)
                                                        .avgPrice(calculateAvgPrice(revenue, quantity))
                                                        .build();
                                })
                                .sorted(Comparator.comparing(AdminReportAnalyticsResponse.RevenueRow::getTotalRevenue)
                                                .reversed())
                                .toList();

                BigDecimal totalQuantity = rows.stream()
                                .map(AdminReportAnalyticsResponse.RevenueRow::getTotalQuantity)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalRevenue = rows.stream()
                                .map(AdminReportAnalyticsResponse.RevenueRow::getTotalRevenue)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                return AdminReportAnalyticsResponse.RevenueTabResponse.builder()
                                .tableRows(rows)
                                .chartSeries(rows)
                                .totals(AdminReportAnalyticsResponse.RevenueTotals.builder()
                                                .totalQuantity(totalQuantity)
                                                .totalRevenue(totalRevenue)
                                                .avgPrice(calculateAvgPrice(totalRevenue, totalQuantity))
                                                .build())
                                .build();
        }

        /**
         * Profit tab analytics: profit by crop and plot.
         */
        public AdminReportAnalyticsResponse.ProfitTabResponse getProfitAnalytics(AdminReportFilter filter) {
                log.info("Generating profit analytics with filter: {}", filter);
                validateFilters(filter);

                LocalDate fromDate = filter.getEffectiveFromDate();
                LocalDate toDate = filter.getEffectiveToDateInclusive();

                Map<CropPlotKey, AdminReportAnalyticsResponse.ProfitRow> rowMap = new LinkedHashMap<>();

                for (AdminReportProjections.RevenueGroupAgg agg : harvestRepository.sumRevenueByCropPlot(
                                fromDate,
                                toDate,
                                filter.getFarmId(),
                                filter.getPlotId(),
                                filter.getCropId(),
                                filter.getVarietyId())) {
                        CropPlotKey key = new CropPlotKey(agg.getCropId(), agg.getPlotId());
                        AdminReportAnalyticsResponse.ProfitRow row = rowMap.get(key);
                        if (row == null) {
                                row = AdminReportAnalyticsResponse.ProfitRow.builder()
                                                .cropId(agg.getCropId())
                                                .cropName(agg.getCropName())
                                                .plotId(agg.getPlotId())
                                                .plotName(agg.getPlotName())
                                                .totalRevenue(BigDecimal.ZERO)
                                                .totalCost(BigDecimal.ZERO)
                                                .grossProfit(BigDecimal.ZERO)
                                                .build();
                                rowMap.put(key, row);
                        }
                        row.setTotalRevenue(zeroIfNull(agg.getTotalRevenue()));
                }

                for (AdminReportProjections.CostCropPlotAgg agg : expenseRepository.sumCostByCropPlot(
                                fromDate,
                                toDate,
                                filter.getFarmId(),
                                filter.getPlotId(),
                                filter.getCropId(),
                                filter.getVarietyId())) {
                        CropPlotKey key = new CropPlotKey(agg.getCropId(), agg.getPlotId());
                        AdminReportAnalyticsResponse.ProfitRow row = rowMap.get(key);
                        if (row == null) {
                                row = AdminReportAnalyticsResponse.ProfitRow.builder()
                                                .cropId(agg.getCropId())
                                                .cropName(agg.getCropName())
                                                .plotId(agg.getPlotId())
                                                .plotName(agg.getPlotName())
                                                .totalRevenue(BigDecimal.ZERO)
                                                .totalCost(BigDecimal.ZERO)
                                                .grossProfit(BigDecimal.ZERO)
                                                .build();
                                rowMap.put(key, row);
                        }
                        row.setTotalCost(zeroIfNull(agg.getTotalCost()));
                        if (row.getCropName() == null) {
                                row.setCropName(agg.getCropName());
                        }
                        if (row.getPlotName() == null) {
                                row.setPlotName(agg.getPlotName());
                        }
                }

                List<AdminReportAnalyticsResponse.ProfitRow> rows = rowMap.values().stream()
                                .peek(row -> {
                                        BigDecimal revenue = zeroIfNull(row.getTotalRevenue());
                                        BigDecimal cost = zeroIfNull(row.getTotalCost());
                                        BigDecimal profit = revenue.subtract(cost);
                                        row.setGrossProfit(profit);
                                        row.setMarginPercent(calculatePercentage(profit, revenue));
                                })
                                .sorted(Comparator.comparing(AdminReportAnalyticsResponse.ProfitRow::getGrossProfit)
                                                .reversed())
                                .toList();

                BigDecimal totalRevenue = rows.stream()
                                .map(AdminReportAnalyticsResponse.ProfitRow::getTotalRevenue)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalCost = rows.stream()
                                .map(AdminReportAnalyticsResponse.ProfitRow::getTotalCost)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal grossProfit = totalRevenue.subtract(totalCost);

                return AdminReportAnalyticsResponse.ProfitTabResponse.builder()
                                .tableRows(rows)
                                .chartSeries(rows)
                                .totals(AdminReportAnalyticsResponse.ProfitTotals.builder()
                                                .totalRevenue(totalRevenue)
                                                .totalCost(totalCost)
                                                .grossProfit(grossProfit)
                                                .marginPercent(calculatePercentage(grossProfit, totalRevenue))
                                                .build())
                                .build();
        }

        /**
         * Export report data to CSV with filter metadata.
         */
        public ReportExport exportReport(AdminReportFilter filter, AdminReportTab tab,
                        ExpenseTimeGranularity granularity) {
                if (tab == null) {
                        throw new AppException(ErrorCode.BAD_REQUEST);
                }
                validateFilters(filter);

                StringBuilder sb = new StringBuilder();
                appendMetadata(sb, tab, filter, granularity);

                switch (tab) {
                        case SUMMARY -> appendSummaryCsv(sb, getSummary(filter));
                        case YIELD -> appendYieldCsv(sb, getYieldAnalytics(filter));
                        case COST -> appendCostCsv(sb, getCostAnalytics(filter, granularity));
                        case REVENUE -> appendRevenueCsv(sb, getRevenueAnalytics(filter));
                        case PROFIT -> appendProfitCsv(sb, getProfitAnalytics(filter));
                        default -> throw new AppException(ErrorCode.BAD_REQUEST);
                }

                String filename = buildExportFilename(tab, filter);
                return new ReportExport(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), filename);
        }

        /**
         * Options for report filters (admin global view).
         */
        public AdminReportOptionsResponse getReportOptions() {
                List<AdminReportOptionsResponse.SimpleOption> farms = farmRepository.findAll().stream()
                                .sorted(Comparator.comparing(
                                                (Farm farm) -> farm.getName() != null ? farm.getName() : ""))
                                .map(farm -> AdminReportOptionsResponse.SimpleOption.builder()
                                                .id(farm.getId())
                                                .name(farm.getName())
                                                .build())
                                .toList();

                List<AdminReportOptionsResponse.PlotOption> plots = plotRepository.findAll().stream()
                                .sorted(Comparator.comparing(
                                                (Plot plot) -> plot.getPlotName() != null ? plot.getPlotName() : ""))
                                .map(plot -> AdminReportOptionsResponse.PlotOption.builder()
                                                .id(plot.getId())
                                                .name(plot.getPlotName())
                                                .farmId(plot.getFarm() != null ? plot.getFarm().getId() : null)
                                                .farmName(plot.getFarm() != null ? plot.getFarm().getName() : null)
                                                .build())
                                .toList();

                List<AdminReportOptionsResponse.SimpleOption> crops = cropRepository.findAll().stream()
                                .sorted(Comparator.comparing(
                                                (Crop crop) -> crop.getCropName() != null ? crop.getCropName() : ""))
                                .map(crop -> AdminReportOptionsResponse.SimpleOption.builder()
                                                .id(crop.getId())
                                                .name(crop.getCropName())
                                                .build())
                                .toList();

                List<AdminReportOptionsResponse.VarietyOption> varieties = varietyRepository.findAll().stream()
                                .sorted(Comparator.comparing(
                                                (Variety variety) -> variety.getName() != null ? variety.getName() : ""))
                                .map(variety -> AdminReportOptionsResponse.VarietyOption.builder()
                                                .id(variety.getId())
                                                .name(variety.getName())
                                                .cropId(variety.getCrop() != null ? variety.getCrop().getId() : null)
                                                .cropName(variety.getCrop() != null ? variety.getCrop().getCropName() : null)
                                                .build())
                                .toList();

                return AdminReportOptionsResponse.builder()
                                .farms(farms)
                                .plots(plots)
                                .crops(crops)
                                .varieties(varieties)
                                .build();
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

        private void validateFilters(AdminReportFilter filter) {
                if (filter == null) {
                        return;
                }
                LocalDate fromDate = filter.getEffectiveFromDate();
                LocalDate toDate = filter.getEffectiveToDateInclusive();
                if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
                        throw new AppException(ErrorCode.INVALID_DATE_RANGE);
                }
                if (filter.getFarmId() != null && filter.getPlotId() != null) {
                        Plot plot = plotRepository.findById(filter.getPlotId())
                                        .orElseThrow(() -> new AppException(ErrorCode.PLOT_NOT_FOUND));
                        if (plot.getFarm() == null || !plot.getFarm().getId().equals(filter.getFarmId())) {
                                throw new AppException(ErrorCode.BAD_REQUEST);
                        }
                }
                if (filter.getCropId() != null && filter.getVarietyId() != null) {
                        Variety variety = varietyRepository.findById(filter.getVarietyId())
                                        .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
                        if (variety.getCrop() == null || !variety.getCrop().getId().equals(filter.getCropId())) {
                                throw new AppException(ErrorCode.BAD_REQUEST);
                        }
                }
        }

        private AdminReportAnalyticsResponse.AppliedFilters buildAppliedFilters(AdminReportFilter filter) {
                if (filter == null) {
                        return AdminReportAnalyticsResponse.AppliedFilters.builder().build();
                }
                return AdminReportAnalyticsResponse.AppliedFilters.builder()
                                .year(filter.getYear())
                                .dateFrom(filter.getEffectiveFromDate())
                                .dateTo(filter.getEffectiveToDateInclusive())
                                .farmId(filter.getFarmId())
                                .plotId(filter.getPlotId())
                                .cropId(filter.getCropId())
                                .varietyId(filter.getVarietyId())
                                .build();
        }

        private BigDecimal zeroIfNull(BigDecimal value) {
                return value != null ? value : BigDecimal.ZERO;
        }

        private String normalizeCategory(String category) {
                return category == null || category.isBlank() ? "Uncategorized" : category;
        }

        private String normalizeVendor(String vendorName) {
                return vendorName == null || vendorName.isBlank() ? "Unassigned" : vendorName;
        }

        private List<AdminReportAnalyticsResponse.CostTimeRow> buildTimeSeries(
                        List<AdminReportProjections.CostTimeAgg> dailyAggs,
                        ExpenseTimeGranularity granularity,
                        LocalDate fromDate,
                        LocalDate toDate) {
                Map<LocalDate, BigDecimal> aggregated = new TreeMap<>();
                for (AdminReportProjections.CostTimeAgg agg : dailyAggs) {
                        LocalDate key = resolveTimeKey(agg.getPeriodStart(), granularity);
                        aggregated.merge(key, zeroIfNull(agg.getTotalCost()), BigDecimal::add);
                }

                if (fromDate != null && toDate != null) {
                        LocalDate cursor = resolveTimeKey(fromDate, granularity);
                        LocalDate end = resolveTimeKey(toDate, granularity);
                        while (!cursor.isAfter(end)) {
                                aggregated.putIfAbsent(cursor, BigDecimal.ZERO);
                                cursor = incrementPeriod(cursor, granularity);
                        }
                }

                return aggregated.entrySet().stream()
                                .map(entry -> AdminReportAnalyticsResponse.CostTimeRow.builder()
                                                .periodStart(entry.getKey())
                                                .label(formatTimeLabel(entry.getKey(), granularity))
                                                .totalCost(entry.getValue())
                                                .build())
                                .toList();
        }

        private LocalDate resolveTimeKey(LocalDate date, ExpenseTimeGranularity granularity) {
                if (date == null) {
                        return null;
                }
                return switch (granularity) {
                        case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                        case MONTH -> date.withDayOfMonth(1);
                        case DAY -> date;
                };
        }

        private LocalDate incrementPeriod(LocalDate date, ExpenseTimeGranularity granularity) {
                return switch (granularity) {
                        case WEEK -> date.plusWeeks(1);
                        case MONTH -> date.plusMonths(1);
                        case DAY -> date.plusDays(1);
                };
        }

        private String formatTimeLabel(LocalDate date, ExpenseTimeGranularity granularity) {
                if (date == null) {
                        return "";
                }
                return switch (granularity) {
                        case WEEK -> {
                                WeekFields wf = WeekFields.ISO;
                                int weekYear = date.get(wf.weekBasedYear());
                                int week = date.get(wf.weekOfWeekBasedYear());
                                yield String.format("%d-W%02d", weekYear, week);
                        }
                        case MONTH -> YearMonth.from(date).toString();
                        case DAY -> date.toString();
                };
        }

        private void appendMetadata(StringBuilder sb, AdminReportTab tab, AdminReportFilter filter,
                        ExpenseTimeGranularity granularity) {
                AdminReportAnalyticsResponse.AppliedFilters applied = buildAppliedFilters(filter);
                sb.append("Report,").append(tab.name()).append("\n");
                sb.append("Year,").append(applied.getYear() != null ? applied.getYear() : "").append("\n");
                sb.append("DateFrom,").append(formatDate(applied.getDateFrom())).append("\n");
                sb.append("DateTo,").append(formatDate(applied.getDateTo())).append("\n");
                sb.append("FarmId,").append(applied.getFarmId() != null ? applied.getFarmId() : "").append("\n");
                sb.append("PlotId,").append(applied.getPlotId() != null ? applied.getPlotId() : "").append("\n");
                sb.append("CropId,").append(applied.getCropId() != null ? applied.getCropId() : "").append("\n");
                sb.append("VarietyId,").append(applied.getVarietyId() != null ? applied.getVarietyId() : "").append("\n");
                if (tab == AdminReportTab.COST && granularity != null) {
                        sb.append("Granularity,").append(granularity.name()).append("\n");
                }
                sb.append("\n");
        }

        private void appendSummaryCsv(StringBuilder sb, AdminReportAnalyticsResponse.Summary summary) {
                sb.append("Metric,Value\n");
                sb.append("Actual Yield,").append(formatDecimal(summary.getActualYield())).append("\n");
                sb.append("Total Cost,").append(formatDecimal(summary.getTotalCost())).append("\n");
                sb.append("Cost Per Ton,").append(formatDecimal(summary.getCostPerTon())).append("\n");
                sb.append("Revenue,").append(formatDecimal(summary.getRevenue())).append("\n");
                sb.append("Gross Profit,").append(formatDecimal(summary.getGrossProfit())).append("\n");
                sb.append("Margin Percent,").append(formatDecimal(summary.getMarginPercent())).append("\n");
                if (summary.getWarnings() != null && !summary.getWarnings().isEmpty()) {
                        sb.append("\n");
                        sb.append("Warnings\n");
                        for (String warning : summary.getWarnings()) {
                                sb.append(escapeCsv(warning)).append("\n");
                        }
                }
        }

        private void appendYieldCsv(StringBuilder sb, AdminReportAnalyticsResponse.YieldTabResponse report) {
                sb.append("Farm,Plot,Crop,Variety,ActualYield,HarvestCount\n");
                for (AdminReportAnalyticsResponse.YieldRow row : report.getTableRows()) {
                        sb.append(escapeCsv(row.getFarmName())).append(',')
                                        .append(escapeCsv(row.getPlotName())).append(',')
                                        .append(escapeCsv(row.getCropName())).append(',')
                                        .append(escapeCsv(row.getVarietyName())).append(',')
                                        .append(formatDecimal(row.getActualYield())).append(',')
                                        .append(row.getHarvestCount() != null ? row.getHarvestCount() : "")
                                        .append('\n');
                }
        }

        private void appendCostCsv(StringBuilder sb, AdminReportAnalyticsResponse.CostTabResponse report) {
                sb.append("Section,Category Breakdown\n");
                sb.append("Category,TotalCost,ExpenseCount\n");
                for (AdminReportAnalyticsResponse.CostCategoryRow row : report.getTableRows()) {
                        sb.append(escapeCsv(row.getCategory())).append(',')
                                        .append(formatDecimal(row.getTotalCost())).append(',')
                                        .append(row.getExpenseCount() != null ? row.getExpenseCount() : "")
                                        .append('\n');
                }
                sb.append("\n");
                sb.append("Section,Vendor Breakdown\n");
                sb.append("Vendor,TotalCost,ExpenseCount\n");
                for (AdminReportAnalyticsResponse.CostVendorRow row : report.getVendorRows()) {
                        sb.append(escapeCsv(row.getVendorName())).append(',')
                                        .append(formatDecimal(row.getTotalCost())).append(',')
                                        .append(row.getExpenseCount() != null ? row.getExpenseCount() : "")
                                        .append('\n');
                }
                sb.append("\n");
                sb.append("Section,Time Series\n");
                sb.append("PeriodStart,Label,TotalCost\n");
                for (AdminReportAnalyticsResponse.CostTimeRow row : report.getTimeSeries()) {
                        sb.append(formatDate(row.getPeriodStart())).append(',')
                                        .append(escapeCsv(row.getLabel())).append(',')
                                        .append(formatDecimal(row.getTotalCost()))
                                        .append('\n');
                }
        }

        private void appendRevenueCsv(StringBuilder sb, AdminReportAnalyticsResponse.RevenueTabResponse report) {
                sb.append("Crop,Plot,TotalQuantity,TotalRevenue,AvgPrice\n");
                for (AdminReportAnalyticsResponse.RevenueRow row : report.getTableRows()) {
                        sb.append(escapeCsv(row.getCropName())).append(',')
                                        .append(escapeCsv(row.getPlotName())).append(',')
                                        .append(formatDecimal(row.getTotalQuantity())).append(',')
                                        .append(formatDecimal(row.getTotalRevenue())).append(',')
                                        .append(formatDecimal(row.getAvgPrice()))
                                        .append('\n');
                }
        }

        private void appendProfitCsv(StringBuilder sb, AdminReportAnalyticsResponse.ProfitTabResponse report) {
                sb.append("Crop,Plot,TotalRevenue,TotalCost,GrossProfit,MarginPercent\n");
                for (AdminReportAnalyticsResponse.ProfitRow row : report.getTableRows()) {
                        sb.append(escapeCsv(row.getCropName())).append(',')
                                        .append(escapeCsv(row.getPlotName())).append(',')
                                        .append(formatDecimal(row.getTotalRevenue())).append(',')
                                        .append(formatDecimal(row.getTotalCost())).append(',')
                                        .append(formatDecimal(row.getGrossProfit())).append(',')
                                        .append(formatDecimal(row.getMarginPercent()))
                                        .append('\n');
                }
        }

        private String buildExportFilename(AdminReportTab tab, AdminReportFilter filter) {
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
                LocalDate fromDate = filter.getEffectiveFromDate();
                LocalDate toDate = filter.getEffectiveToDateInclusive();
                String range = (fromDate != null && toDate != null)
                                ? fromDate.format(fmt) + "-" + toDate.format(fmt)
                                : "all";
                String farmPart = filter.getFarmId() != null ? "farm-" + filter.getFarmId() : "farm-all";
                String plotPart = filter.getPlotId() != null ? "plot-" + filter.getPlotId() : "plot-all";
                return "reports_" + tab.name().toLowerCase(Locale.ROOT) + "_" + range + "_" + farmPart + "_"
                                + plotPart + ".csv";
        }

        private String formatDate(LocalDate date) {
                return date != null ? date.toString() : "";
        }

        private String formatDecimal(BigDecimal value) {
                return value != null ? value.toPlainString() : "";
        }

        private String escapeCsv(String value) {
                if (value == null) {
                        return "";
                }
                String escaped = value.replace("\"", "\"\"");
                if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r")) {
                        return "\"" + escaped + "\"";
                }
                return escaped;
        }

        public record ReportExport(byte[] data, String filename) {
        }

        private record CropPlotKey(Integer cropId, Integer plotId) {
        }
}
