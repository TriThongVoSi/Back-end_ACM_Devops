package org.example.QuanLyMuaVu.Repository;

import org.example.QuanLyMuaVu.DTO.Response.AdminReportProjections;
import org.example.QuanLyMuaVu.Entity.Crop;
import org.example.QuanLyMuaVu.Entity.Expense;
import org.example.QuanLyMuaVu.Entity.Farm;
import org.example.QuanLyMuaVu.Entity.Plot;
import org.example.QuanLyMuaVu.Entity.Province;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Entity.Ward;
import org.example.QuanLyMuaVu.Enums.PaymentStatus;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class ExpenseRepositoryTest {

    @Autowired
    ExpenseRepository expenseRepository;

    @Autowired
    SeasonRepository seasonRepository;

    @Autowired
    PlotRepository plotRepository;

    @Autowired
    FarmRepository farmRepository;

    @Autowired
    CropRepository cropRepository;

    @Autowired
    ProvinceRepository provinceRepository;

    @Autowired
    WardRepository wardRepository;

    @Autowired
    UserRepository userRepository;

    User user;
    Season seasonOne;
    Season seasonTwo;

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
                .username("farmer")
                .email("farmer@example.com")
                .build());

        Farm farm = farmRepository.save(Farm.builder()
                .name("Farm A")
                .province(province)
                .ward(ward)
                .active(true)
                .user(user)
                .build());

        Plot plot = plotRepository.save(Plot.builder()
                .plotName("Plot A")
                .farm(farm)
                .user(user)
                .build());

        Crop crop = cropRepository.save(Crop.builder()
                .cropName("Rice")
                .build());

        seasonOne = seasonRepository.save(Season.builder()
                .seasonName("Season One")
                .plot(plot)
                .crop(crop)
                .startDate(LocalDate.of(2026, 1, 1))
                .plannedHarvestDate(LocalDate.of(2026, 3, 1))
                .status(SeasonStatus.ACTIVE)
                .initialPlantCount(100)
                .build());

        seasonTwo = seasonRepository.save(Season.builder()
                .seasonName("Season Two")
                .plot(plot)
                .crop(crop)
                .startDate(LocalDate.of(2026, 4, 1))
                .plannedHarvestDate(LocalDate.of(2026, 6, 1))
                .status(SeasonStatus.ACTIVE)
                .initialPlantCount(120)
                .build());
    }

    @Test
    @DisplayName("sumTotalCostBySeasonId uses amount/totalCost and ignores deleted")
    void sumTotalCostBySeasonId_excludesDeleted() {
        saveExpense(seasonOne, BigDecimal.valueOf(100), BigDecimal.valueOf(100), PaymentStatus.PAID, false, false);
        saveExpense(seasonOne, null, BigDecimal.valueOf(200), PaymentStatus.PENDING, false, false);
        saveExpense(seasonOne, BigDecimal.valueOf(50), BigDecimal.valueOf(50), PaymentStatus.PAID, true, false);

        BigDecimal sum = expenseRepository.sumTotalCostBySeasonId(seasonOne.getId());

        assertEquals(0, sum.compareTo(BigDecimal.valueOf(300)));
    }

    @Test
    @DisplayName("sumEffectiveAmountBySeasonIdAndStatus filters by status")
    void sumEffectiveAmountBySeasonIdAndStatus_filtersByStatus() {
        saveExpense(seasonOne, BigDecimal.valueOf(80), BigDecimal.valueOf(80), PaymentStatus.PAID, false, false);
        saveExpense(seasonOne, BigDecimal.valueOf(50), BigDecimal.valueOf(50), PaymentStatus.PENDING, false, false);
        saveExpense(seasonOne, BigDecimal.valueOf(20), BigDecimal.valueOf(20), PaymentStatus.PAID, false, false);

        BigDecimal paidSum = expenseRepository.sumEffectiveAmountBySeasonIdAndStatus(seasonOne.getId(), PaymentStatus.PAID);

        assertEquals(0, paidSum.compareTo(BigDecimal.valueOf(100)));
    }

    @Test
    @DisplayName("sumExpensesBySeasonIds groups totals by season")
    void sumExpensesBySeasonIds_groupsBySeason() {
        saveExpense(seasonOne, BigDecimal.valueOf(100), BigDecimal.valueOf(100), PaymentStatus.PAID, false, false);
        saveExpense(seasonTwo, BigDecimal.valueOf(300), BigDecimal.valueOf(300), PaymentStatus.PAID, false, false);

        Map<Integer, BigDecimal> totals = expenseRepository.sumExpensesBySeasonIds(
                        Set.of(seasonOne.getId(), seasonTwo.getId()))
                .stream()
                .collect(Collectors.toMap(
                        AdminReportProjections.SeasonExpenseAgg::getSeasonId,
                        AdminReportProjections.SeasonExpenseAgg::getTotalExpense));

        assertEquals(0, totals.get(seasonOne.getId()).compareTo(BigDecimal.valueOf(100)));
        assertEquals(0, totals.get(seasonTwo.getId()).compareTo(BigDecimal.valueOf(300)));
    }

    @Test
    @DisplayName("countBySeasonIdWithAttachment ignores deleted attachments")
    void countBySeasonIdWithAttachment_ignoresDeleted() {
        saveExpense(seasonOne, BigDecimal.valueOf(10), BigDecimal.valueOf(10), PaymentStatus.PAID, false, true);
        saveExpense(seasonOne, BigDecimal.valueOf(20), BigDecimal.valueOf(20), PaymentStatus.PAID, true, true);
        saveExpense(seasonOne, BigDecimal.valueOf(30), BigDecimal.valueOf(30), PaymentStatus.PAID, false, false);

        long count = expenseRepository.countBySeasonIdWithAttachment(seasonOne.getId());

        assertEquals(1L, count);
    }

    private Expense saveExpense(
            Season season,
            BigDecimal amount,
            BigDecimal totalCost,
            PaymentStatus status,
            boolean deleted,
            boolean withAttachment
    ) {
        Expense expense = Expense.builder()
                .user(user)
                .season(season)
                .itemName("Item")
                .unitPrice(totalCost != null ? totalCost : BigDecimal.valueOf(10))
                .quantity(1)
                .totalCost(totalCost)
                .amount(amount)
                .paymentStatus(status)
                .expenseDate(LocalDate.of(2026, 1, 10))
                .attachmentUrl(withAttachment ? "http://example.com/receipt" : null)
                .deletedAt(deleted ? LocalDateTime.now() : null)
                .build();
        return expenseRepository.save(expense);
    }
}
