package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.DTO.Request.CreateExpenseRequest;
import org.example.QuanLyMuaVu.DTO.Request.UpdateExpenseRequest;
import org.example.QuanLyMuaVu.DTO.Response.BudgetTrackerResponse;
import org.example.QuanLyMuaVu.DTO.Response.ExpenseResponse;
import org.example.QuanLyMuaVu.Entity.Crop;
import org.example.QuanLyMuaVu.Entity.Expense;
import org.example.QuanLyMuaVu.Entity.Plot;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Entity.Supplier;
import org.example.QuanLyMuaVu.Entity.Task;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Enums.PaymentStatus;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.ExpenseRepository;
import org.example.QuanLyMuaVu.Repository.SeasonRepository;
import org.example.QuanLyMuaVu.Repository.SupplierRepository;
import org.example.QuanLyMuaVu.Repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeasonExpenseServiceTest {

    @Mock
    ExpenseRepository expenseRepository;

    @Mock
    SeasonRepository seasonRepository;

    @Mock
    TaskRepository taskRepository;

    @Mock
    SupplierRepository supplierRepository;

    @Mock
    FarmAccessService farmAccessService;

    @InjectMocks
    SeasonExpenseService seasonExpenseService;

    User user;
    Plot plot;
    Season season;
    Task task;
    Supplier supplier;
    Expense expense;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("farmer")
                .build();

        plot = Plot.builder()
                .id(10)
                .plotName("Plot A")
                .build();

        Crop crop = Crop.builder()
                .id(3)
                .cropName("Rice")
                .build();

        season = Season.builder()
                .id(5)
                .seasonName("Season A")
                .plot(plot)
                .crop(crop)
                .startDate(LocalDate.of(2026, 1, 1))
                .plannedHarvestDate(LocalDate.of(2026, 3, 1))
                .status(SeasonStatus.ACTIVE)
                .initialPlantCount(100)
                .build();

        task = Task.builder()
                .id(7)
                .title("Weeding")
                .season(season)
                .user(user)
                .build();

        supplier = Supplier.builder()
                .id(9)
                .name("Vendor")
                .build();

        expense = Expense.builder()
                .id(11)
                .user(user)
                .season(season)
                .task(task)
                .supplier(supplier)
                .category("Seeds")
                .amount(BigDecimal.valueOf(120))
                .paymentStatus(PaymentStatus.PAID)
                .note("note")
                .itemName("Seed")
                .unitPrice(BigDecimal.valueOf(120))
                .quantity(1)
                .totalCost(BigDecimal.valueOf(120))
                .expenseDate(LocalDate.of(2026, 1, 10))
                .build();
    }

    @Test
    @DisplayName("CreateExpense - saves expense with supplier, task, and payment status")
    void createExpense_validRequest_savesExpense() {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .amount(BigDecimal.valueOf(250))
                .expenseDate(LocalDate.of(2026, 1, 12))
                .category("Seeds")
                .plotId(plot.getId())
                .paymentStatus(PaymentStatus.PAID)
                .taskId(task.getId())
                .vendorId(supplier.getId())
                .note("seed purchase")
                .itemName("Hybrid seeds")
                .build();

        when(seasonRepository.findById(season.getId())).thenReturn(Optional.of(season));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(season);
        when(farmAccessService.getCurrentUser()).thenReturn(user);
        when(taskRepository.existsByIdAndSeasonId(task.getId(), season.getId())).thenReturn(true);
        when(taskRepository.findByIdAndSeasonId(task.getId(), season.getId())).thenReturn(Optional.of(task));
        when(supplierRepository.findById(supplier.getId())).thenReturn(Optional.of(supplier));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense saved = invocation.getArgument(0);
            saved.setId(99);
            return saved;
        });

        ExpenseResponse response = seasonExpenseService.CreateExpense(season.getId(), request);

        assertEquals(99, response.getId());
        assertEquals(PaymentStatus.PAID, response.getPaymentStatus());
        assertEquals(supplier.getId(), response.getVendorId());

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        Expense saved = captor.getValue();
        assertEquals(BigDecimal.valueOf(250), saved.getAmount());
        assertEquals(BigDecimal.valueOf(250), saved.getUnitPrice());
        assertEquals(1, saved.getQuantity());
        assertNotNull(saved.getExpenseDate());
    }

    @Test
    @DisplayName("CreateExpense - invalid amount throws MSG_4_INVALID_FORMAT")
    void createExpense_invalidAmount_throws() {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .amount(BigDecimal.ZERO)
                .expenseDate(LocalDate.of(2026, 1, 12))
                .category("Seeds")
                .plotId(plot.getId())
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(seasonRepository.findById(season.getId())).thenReturn(Optional.of(season));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(season);

        AppException exception = assertThrows(AppException.class,
                () -> seasonExpenseService.CreateExpense(season.getId(), request));

        assertEquals(ErrorCode.MSG_4_INVALID_FORMAT, exception.getErrorCode());
        verify(expenseRepository, never()).save(any());
    }

    @Test
    @DisplayName("CreateExpense - task not in season throws MSG_9_CONSTRAINT_VIOLATION")
    void createExpense_taskNotInSeason_throws() {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .amount(BigDecimal.valueOf(50))
                .expenseDate(LocalDate.of(2026, 1, 5))
                .category("Labor")
                .plotId(plot.getId())
                .paymentStatus(PaymentStatus.PENDING)
                .taskId(task.getId())
                .build();

        when(seasonRepository.findById(season.getId())).thenReturn(Optional.of(season));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(season);
        when(taskRepository.existsByIdAndSeasonId(task.getId(), season.getId())).thenReturn(false);

        AppException exception = assertThrows(AppException.class,
                () -> seasonExpenseService.CreateExpense(season.getId(), request));

        assertEquals(ErrorCode.MSG_9_CONSTRAINT_VIOLATION, exception.getErrorCode());
    }

    @Test
    @DisplayName("UpdateExpense - updates expense fields")
    void updateExpense_valid_updatesFields() {
        UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                .amount(BigDecimal.valueOf(300))
                .expenseDate(LocalDate.of(2026, 1, 15))
                .category("Equipment")
                .seasonId(season.getId())
                .plotId(plot.getId())
                .paymentStatus(PaymentStatus.PENDING)
                .taskId(task.getId())
                .vendorId(supplier.getId())
                .note("updated")
                .itemName("Updated item")
                .build();

        when(expenseRepository.findById(expense.getId())).thenReturn(Optional.of(expense));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(season);
        when(seasonRepository.findById(season.getId())).thenReturn(Optional.of(season));
        when(taskRepository.existsByIdAndSeasonId(task.getId(), season.getId())).thenReturn(true);
        when(taskRepository.findByIdAndSeasonId(task.getId(), season.getId())).thenReturn(Optional.of(task));
        when(supplierRepository.findById(supplier.getId())).thenReturn(Optional.of(supplier));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseResponse response = seasonExpenseService.UpdateExpense(expense.getId(), request);

        assertEquals("Equipment", response.getCategory());
        assertEquals(PaymentStatus.PENDING, response.getPaymentStatus());
        assertEquals(BigDecimal.valueOf(300), response.getAmount());
    }

    @Test
    @DisplayName("UpdateExpense - locked season throws EXPENSE_PERIOD_LOCKED")
    void updateExpense_lockedSeason_throws() {
        Season lockedSeason = Season.builder()
                .id(6)
                .seasonName("Locked")
                .plot(plot)
                .crop(season.getCrop())
                .startDate(LocalDate.of(2026, 1, 1))
                .plannedHarvestDate(LocalDate.of(2026, 3, 1))
                .status(SeasonStatus.COMPLETED)
                .initialPlantCount(100)
                .build();

        UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                .amount(BigDecimal.valueOf(100))
                .expenseDate(LocalDate.of(2026, 1, 10))
                .category("Seeds")
                .seasonId(lockedSeason.getId())
                .plotId(plot.getId())
                .paymentStatus(PaymentStatus.PAID)
                .build();

        when(expenseRepository.findById(expense.getId())).thenReturn(Optional.of(expense));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(season);
        when(seasonRepository.findById(lockedSeason.getId())).thenReturn(Optional.of(lockedSeason));

        AppException exception = assertThrows(AppException.class,
                () -> seasonExpenseService.UpdateExpense(expense.getId(), request));

        assertEquals(ErrorCode.EXPENSE_PERIOD_LOCKED, exception.getErrorCode());
    }

    @Test
    @DisplayName("DeleteExpense - sets deletedAt timestamp")
    void deleteExpense_setsDeletedAt() {
        when(expenseRepository.findById(expense.getId())).thenReturn(Optional.of(expense));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(season);
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        seasonExpenseService.DeleteExpense(expense.getId());

        assertNotNull(expense.getDeletedAt());
    }

    @Test
    @DisplayName("getExpense - deleted expense throws MSG_10_EXPENSE_NOT_FOUND")
    void getExpense_deletedExpense_throws() {
        expense.setDeletedAt(LocalDateTime.now());
        when(expenseRepository.findById(expense.getId())).thenReturn(Optional.of(expense));

        AppException exception = assertThrows(AppException.class,
                () -> seasonExpenseService.getExpense(expense.getId()));

        assertEquals(ErrorCode.MSG_10_EXPENSE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("getExpense - access denied propagates")
    void getExpense_accessDenied_throws() {
        when(expenseRepository.findById(expense.getId())).thenReturn(Optional.of(expense));
        doThrow(new AppException(ErrorCode.FORBIDDEN))
                .when(farmAccessService).assertCurrentUserCanAccessSeason(season);

        AppException exception = assertThrows(AppException.class,
                () -> seasonExpenseService.getExpense(expense.getId()));

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("Budget tracker - null budget does not compute usage")
    void budgetTracker_budgetNull_returnsNullUsage() {
        Season noBudgetSeason = Season.builder()
                .id(7)
                .seasonName("No Budget")
                .plot(plot)
                .crop(season.getCrop())
                .startDate(LocalDate.of(2026, 1, 1))
                .plannedHarvestDate(LocalDate.of(2026, 3, 1))
                .status(SeasonStatus.ACTIVE)
                .initialPlantCount(100)
                .budgetAmount(null)
                .build();

        when(seasonRepository.findById(noBudgetSeason.getId())).thenReturn(Optional.of(noBudgetSeason));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(noBudgetSeason);
        when(expenseRepository.sumTotalCostBySeasonId(noBudgetSeason.getId())).thenReturn(BigDecimal.valueOf(100));
        when(expenseRepository.sumEffectiveAmountBySeasonIdAndStatus(noBudgetSeason.getId(), PaymentStatus.PAID))
                .thenReturn(BigDecimal.valueOf(60));
        when(expenseRepository.sumEffectiveAmountBySeasonIdAndStatus(noBudgetSeason.getId(), PaymentStatus.PENDING))
                .thenReturn(BigDecimal.valueOf(20));
        when(expenseRepository.sumEffectiveAmountBySeasonIdAndStatus(noBudgetSeason.getId(), PaymentStatus.UNPAID))
                .thenReturn(BigDecimal.valueOf(20));
        when(expenseRepository.countBySeasonId(noBudgetSeason.getId())).thenReturn(2L);
        when(expenseRepository.countBySeasonIdWithAttachment(noBudgetSeason.getId())).thenReturn(1L);

        BudgetTrackerResponse response = seasonExpenseService.getBudgetTracker(noBudgetSeason.getId());

        assertEquals(BigDecimal.valueOf(100), response.getTotal());
        assertEquals(BigDecimal.valueOf(60), response.getPaid());
        assertEquals(BigDecimal.valueOf(40), response.getUnpaid());
        assertEquals(null, response.getUsagePercent());
        assertEquals(null, response.getRemaining());
    }

    @Test
    @DisplayName("Budget tracker - overspent budget computes negative remaining")
    void budgetTracker_overspent_computesRemaining() {
        Season budgetSeason = Season.builder()
                .id(8)
                .seasonName("Budgeted")
                .plot(plot)
                .crop(season.getCrop())
                .startDate(LocalDate.of(2026, 1, 1))
                .plannedHarvestDate(LocalDate.of(2026, 3, 1))
                .status(SeasonStatus.ACTIVE)
                .initialPlantCount(100)
                .budgetAmount(BigDecimal.valueOf(100))
                .build();

        when(seasonRepository.findById(budgetSeason.getId())).thenReturn(Optional.of(budgetSeason));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(budgetSeason);
        when(expenseRepository.sumTotalCostBySeasonId(budgetSeason.getId())).thenReturn(BigDecimal.valueOf(150));
        when(expenseRepository.sumEffectiveAmountBySeasonIdAndStatus(budgetSeason.getId(), PaymentStatus.PAID))
                .thenReturn(BigDecimal.valueOf(100));
        when(expenseRepository.sumEffectiveAmountBySeasonIdAndStatus(budgetSeason.getId(), PaymentStatus.PENDING))
                .thenReturn(BigDecimal.valueOf(30));
        when(expenseRepository.sumEffectiveAmountBySeasonIdAndStatus(budgetSeason.getId(), PaymentStatus.UNPAID))
                .thenReturn(BigDecimal.valueOf(20));
        when(expenseRepository.countBySeasonId(budgetSeason.getId())).thenReturn(3L);
        when(expenseRepository.countBySeasonIdWithAttachment(budgetSeason.getId())).thenReturn(2L);

        BudgetTrackerResponse response = seasonExpenseService.getBudgetTracker(budgetSeason.getId());

        assertEquals(new BigDecimal("150.00"), response.getUsagePercent());
        assertEquals(BigDecimal.valueOf(-50), response.getRemaining());
    }
}
