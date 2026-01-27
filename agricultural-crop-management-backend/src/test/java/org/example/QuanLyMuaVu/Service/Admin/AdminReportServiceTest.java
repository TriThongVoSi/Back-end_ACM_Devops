package org.example.QuanLyMuaVu.Service.Admin;

import org.example.QuanLyMuaVu.DTO.Request.AdminReportFilter;
import org.example.QuanLyMuaVu.DTO.Response.AdminReportAnalyticsResponse;
import org.example.QuanLyMuaVu.Entity.Crop;
import org.example.QuanLyMuaVu.Entity.Expense;
import org.example.QuanLyMuaVu.Entity.Farm;
import org.example.QuanLyMuaVu.Entity.Harvest;
import org.example.QuanLyMuaVu.Entity.Plot;
import org.example.QuanLyMuaVu.Entity.Province;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Entity.Supplier;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Entity.Variety;
import org.example.QuanLyMuaVu.Entity.Ward;
import org.example.QuanLyMuaVu.Enums.AdminReportTab;
import org.example.QuanLyMuaVu.Enums.ExpenseTimeGranularity;
import org.example.QuanLyMuaVu.Enums.PaymentStatus;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.CropRepository;
import org.example.QuanLyMuaVu.Repository.ExpenseRepository;
import org.example.QuanLyMuaVu.Repository.FarmRepository;
import org.example.QuanLyMuaVu.Repository.HarvestRepository;
import org.example.QuanLyMuaVu.Repository.PlotRepository;
import org.example.QuanLyMuaVu.Repository.ProvinceRepository;
import org.example.QuanLyMuaVu.Repository.SeasonRepository;
import org.example.QuanLyMuaVu.Repository.SupplierRepository;
import org.example.QuanLyMuaVu.Repository.UserRepository;
import org.example.QuanLyMuaVu.Repository.VarietyRepository;
import org.example.QuanLyMuaVu.Repository.WardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(AdminReportService.class)
class AdminReportServiceTest {

    @Autowired
    AdminReportService adminReportService;

    @Autowired
    ExpenseRepository expenseRepository;

    @Autowired
    HarvestRepository harvestRepository;

    @Autowired
    SeasonRepository seasonRepository;

    @Autowired
    FarmRepository farmRepository;

    @Autowired
    CropRepository cropRepository;

    @Autowired
    PlotRepository plotRepository;

    @Autowired
    VarietyRepository varietyRepository;

    @Autowired
    SupplierRepository supplierRepository;

    @Autowired
    ProvinceRepository provinceRepository;

    @Autowired
    WardRepository wardRepository;

    @Autowired
    UserRepository userRepository;

    User user;
    Farm farmA;
    Farm farmB;
    Plot plotA;
    Plot plotB;
    Crop cropRice;
    Crop cropCorn;
    Variety varietyA;
    Variety varietyB;
    Season seasonA;
    Season seasonB;
    Supplier supplierA;
    Supplier supplierB;

    @BeforeEach
    void setUp() {
        Province province = Province.builder()
                .id(1)
                .name("Test Province")
                .slug("test-province")
                .type("tinh")
                .nameWithType("Test Province")
                .build();
        provinceRepository.save(province);

        Ward ward = Ward.builder()
                .id(10)
                .name("Test Ward")
                .slug("test-ward")
                .type("phuong")
                .nameWithType("Test Ward")
                .province(province)
                .build();
        wardRepository.save(ward);

        user = userRepository.save(User.builder()
                .username("admin")
                .email("admin@example.com")
                .build());

        farmA = farmRepository.save(Farm.builder()
                .name("Farm A")
                .province(province)
                .ward(ward)
                .active(true)
                .user(user)
                .build());

        farmB = farmRepository.save(Farm.builder()
                .name("Farm B")
                .province(province)
                .ward(ward)
                .active(true)
                .user(user)
                .build());

        plotA = plotRepository.save(Plot.builder()
                .plotName("Plot A")
                .farm(farmA)
                .user(user)
                .build());

        plotB = plotRepository.save(Plot.builder()
                .plotName("Plot B")
                .farm(farmB)
                .user(user)
                .build());

        cropRice = cropRepository.save(Crop.builder()
                .cropName("Rice")
                .build());

        cropCorn = cropRepository.save(Crop.builder()
                .cropName("Corn")
                .build());

        varietyA = varietyRepository.save(Variety.builder()
                .crop(cropRice)
                .name("Jasmine")
                .build());

        varietyB = varietyRepository.save(Variety.builder()
                .crop(cropCorn)
                .name("Sweet")
                .build());

        seasonA = seasonRepository.save(Season.builder()
                .seasonName("Season A")
                .plot(plotA)
                .crop(cropRice)
                .variety(varietyA)
                .startDate(LocalDate.of(2026, 1, 1))
                .plannedHarvestDate(LocalDate.of(2026, 3, 1))
                .status(SeasonStatus.ACTIVE)
                .initialPlantCount(100)
                .build());

        seasonB = seasonRepository.save(Season.builder()
                .seasonName("Season B")
                .plot(plotB)
                .crop(cropCorn)
                .variety(varietyB)
                .startDate(LocalDate.of(2026, 1, 1))
                .plannedHarvestDate(LocalDate.of(2026, 3, 1))
                .status(SeasonStatus.ACTIVE)
                .initialPlantCount(120)
                .build());

        supplierA = supplierRepository.save(Supplier.builder()
                .name("Supplier A")
                .build());

        supplierB = supplierRepository.save(Supplier.builder()
                .name("Supplier B")
                .build());
    }

    @Test
    @DisplayName("Summary computes yield, cost, revenue, profit, and margin correctly")
    void summaryComputesCorrectValues() {
        saveHarvest(seasonA, LocalDate.of(2026, 2, 10), BigDecimal.valueOf(100), BigDecimal.valueOf(5));
        saveHarvest(seasonA, LocalDate.of(2026, 2, 12), BigDecimal.valueOf(200), BigDecimal.valueOf(5));
        saveExpense(seasonA, LocalDate.of(2026, 2, 5), BigDecimal.valueOf(500), "Seed", supplierA);
        saveExpense(seasonA, LocalDate.of(2026, 2, 6), BigDecimal.valueOf(700), "Labor", supplierB);

        AdminReportFilter filter = AdminReportFilter.builder()
                .fromDate(LocalDate.of(2026, 2, 1))
                .toDate(LocalDate.of(2026, 2, 28))
                .build();

        AdminReportAnalyticsResponse.Summary summary = adminReportService.getSummary(filter);

        assertEquals(0, summary.getActualYield().compareTo(BigDecimal.valueOf(300)));
        assertEquals(0, summary.getTotalCost().compareTo(BigDecimal.valueOf(1200)));
        assertEquals(0, summary.getCostPerTon().compareTo(new BigDecimal("4.00")));
        assertEquals(0, summary.getRevenue().compareTo(BigDecimal.valueOf(1500)));
        assertEquals(0, summary.getGrossProfit().compareTo(BigDecimal.valueOf(300)));
        assertEquals(0, summary.getMarginPercent().compareTo(new BigDecimal("20.00")));
    }

    @Test
    @DisplayName("costPerTon returns null when yield is zero")
    void costPerTonNullWhenYieldZero() {
        saveExpense(seasonA, LocalDate.of(2026, 3, 5), BigDecimal.valueOf(200), "Seed", supplierA);

        AdminReportFilter filter = AdminReportFilter.builder()
                .fromDate(LocalDate.of(2026, 3, 1))
                .toDate(LocalDate.of(2026, 3, 31))
                .build();

        AdminReportAnalyticsResponse.Summary summary = adminReportService.getSummary(filter);

        assertNull(summary.getCostPerTon());
    }

    @Test
    @DisplayName("marginPercent returns null when revenue is zero")
    void marginNullWhenRevenueZero() {
        saveHarvest(seasonA, LocalDate.of(2026, 4, 10), BigDecimal.valueOf(100), BigDecimal.ZERO);
        saveExpense(seasonA, LocalDate.of(2026, 4, 12), BigDecimal.valueOf(100), "Seed", supplierA);

        AdminReportFilter filter = AdminReportFilter.builder()
                .fromDate(LocalDate.of(2026, 4, 1))
                .toDate(LocalDate.of(2026, 4, 30))
                .build();

        AdminReportAnalyticsResponse.Summary summary = adminReportService.getSummary(filter);

        assertNull(summary.getMarginPercent());
        assertNotNull(summary.getWarnings());
        assertFalse(summary.getWarnings().isEmpty());
    }

    @Test
    @DisplayName("Plot must belong to farm when both filters are provided")
    void plotMustBelongToFarm() {
        AdminReportFilter filter = AdminReportFilter.builder()
                .farmId(farmA.getId())
                .plotId(plotB.getId())
                .build();

        AppException ex = assertThrows(AppException.class, () -> adminReportService.getSummary(filter));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
    }

    @Test
    @DisplayName("Variety must belong to crop when both filters are provided")
    void varietyMustBelongToCrop() {
        AdminReportFilter filter = AdminReportFilter.builder()
                .cropId(cropRice.getId())
                .varietyId(varietyB.getId())
                .build();

        AppException ex = assertThrows(AppException.class, () -> adminReportService.getSummary(filter));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
    }

    @Test
    @DisplayName("Cost analytics groups totals by category")
    void costByCategoryGroupingWorks() {
        saveExpense(seasonA, LocalDate.of(2026, 5, 1), BigDecimal.valueOf(300), "Seed", supplierA);
        saveExpense(seasonA, LocalDate.of(2026, 5, 2), BigDecimal.valueOf(200), "Seed", supplierA);
        saveExpense(seasonA, LocalDate.of(2026, 5, 3), BigDecimal.valueOf(150), "Labor", supplierB);

        AdminReportFilter filter = AdminReportFilter.builder()
                .fromDate(LocalDate.of(2026, 5, 1))
                .toDate(LocalDate.of(2026, 5, 31))
                .build();

        AdminReportAnalyticsResponse.CostTabResponse report = adminReportService.getCostAnalytics(
                filter,
                ExpenseTimeGranularity.MONTH);

        Map<String, BigDecimal> totals = report.getTableRows().stream()
                .collect(Collectors.toMap(
                        row -> row.getCategory(),
                        AdminReportAnalyticsResponse.CostCategoryRow::getTotalCost));

        assertEquals(0, totals.get("Seed").compareTo(BigDecimal.valueOf(500)));
        assertEquals(0, totals.get("Labor").compareTo(BigDecimal.valueOf(150)));
    }

    @Test
    @DisplayName("Export returns CSV headers for summary tab")
    void exportReturnsCsvHeaders() {
        saveHarvest(seasonA, LocalDate.of(2026, 6, 10), BigDecimal.valueOf(50), BigDecimal.valueOf(5));
        saveExpense(seasonA, LocalDate.of(2026, 6, 11), BigDecimal.valueOf(100), "Seed", supplierA);

        AdminReportFilter filter = AdminReportFilter.builder()
                .fromDate(LocalDate.of(2026, 6, 1))
                .toDate(LocalDate.of(2026, 6, 30))
                .build();

        AdminReportService.ReportExport export = adminReportService.exportReport(
                filter,
                AdminReportTab.SUMMARY,
                ExpenseTimeGranularity.MONTH);

        String csv = new String(export.data(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Metric,Value"));
        assertTrue(export.filename().contains("reports_summary"));
    }

    @Test
    @DisplayName("Yield analytics totals sum quantities and harvest count")
    void yieldAnalyticsTotals() {
        saveHarvest(seasonA, LocalDate.of(2026, 7, 5), BigDecimal.valueOf(120), BigDecimal.valueOf(5));
        saveHarvest(seasonA, LocalDate.of(2026, 7, 6), BigDecimal.valueOf(80), BigDecimal.valueOf(5));

        AdminReportFilter filter = AdminReportFilter.builder()
                .fromDate(LocalDate.of(2026, 7, 1))
                .toDate(LocalDate.of(2026, 7, 31))
                .build();

        AdminReportAnalyticsResponse.YieldTabResponse report = adminReportService.getYieldAnalytics(filter);

        assertEquals(0, report.getTotals().getActualYield().compareTo(BigDecimal.valueOf(200)));
        assertEquals(2L, report.getTotals().getHarvestCount());
    }

    private Harvest saveHarvest(Season season, LocalDate date, BigDecimal quantity, BigDecimal unit) {
        Harvest harvest = Harvest.builder()
                .season(season)
                .harvestDate(date)
                .quantity(quantity)
                .unit(unit)
                .build();
        return harvestRepository.save(harvest);
    }

    private Expense saveExpense(Season season, LocalDate date, BigDecimal amount, String category, Supplier supplier) {
        Expense expense = Expense.builder()
                .user(user)
                .season(season)
                .itemName("Item")
                .unitPrice(BigDecimal.ONE)
                .quantity(1)
                .amount(amount)
                .paymentStatus(PaymentStatus.PAID)
                .category(category)
                .supplier(supplier)
                .expenseDate(date)
                .build();
        return expenseRepository.save(expense);
    }
}
