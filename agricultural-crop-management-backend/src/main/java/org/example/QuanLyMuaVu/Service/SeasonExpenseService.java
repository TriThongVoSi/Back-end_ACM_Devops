package org.example.QuanLyMuaVu.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.DTO.Request.CreateExpenseRequest;
import org.example.QuanLyMuaVu.DTO.Request.ExpenseSearchCriteria;
import org.example.QuanLyMuaVu.DTO.Request.UpdateExpenseRequest;
import org.example.QuanLyMuaVu.DTO.Response.ExpenseResponse;
import org.example.QuanLyMuaVu.Entity.Expense;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Entity.Task;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.ExpenseRepository;
import org.example.QuanLyMuaVu.Repository.SeasonRepository;
import org.example.QuanLyMuaVu.Repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * BR174-BR187: Season Expense Service
 * Handles all expense CRUD operations with BR-compliant validations.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class SeasonExpenseService {

    ExpenseRepository expenseRepository;
    SeasonRepository seasonRepository;
    TaskRepository taskRepository;
    FarmAccessService farmAccessService;

    // ═══════════════════════════════════════════════════════════════════════════
    // BR176: CreateExpense(Expense expense) - Create Expense with Full Validation
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * BR176: CreateExpense method with parameter "expense" as an object class.
     * Validates all constraints before saving to database.
     *
     * @param seasonId the season ID from path variable [cmbSeasonID]
     * @param request  the expense creation request containing all form fields
     * @return ExpenseResponse on success (MSG 7)
     * @throws AppException with MSG 9 on constraint violation
     */
    public ExpenseResponse CreateExpense(Integer seasonId, CreateExpenseRequest request) {
        // BR175: ValidateDataFormat() - Input validation done via @Valid annotations

        // BR176: Validate season exists and farmer has access
        Season season = getSeasonForCurrentFarmer(seasonId);

        // BR176: Validate season is not closed/archived
        ensureSeasonOpenForExpenses(season);

        // BR176: Validate [Expense.season_id] is consistent with [Expense.plot_id]
        validateSeasonBelongsToPlot(season, request.getPlotId());

        // BR176: Validate [Expense.task_id] belongs to Season/Plot if provided
        Task task = null;
        if (request.getTaskId() != null) {
            task = validateTaskBelongsToSeason(request.getTaskId(), seasonId);
        }

        // BR175: Validate amount > 0
        validateAmount(request.getAmount());

        // Validate expense date within season dates
        validateExpenseDateWithinSeason(season, request.getExpenseDate());

        User currentUser = getCurrentUser();

        // Calculate totalCost for legacy compatibility
        BigDecimal totalCost = request.getAmount();
        if (request.getUnitPrice() != null && request.getQuantity() != null) {
            totalCost = request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        }

        // Determine itemName (use category if not provided)
        String itemName = request.getItemName();
        if (itemName == null || itemName.isBlank()) {
            itemName = request.getCategory() != null ? request.getCategory() : "Expense";
        }

        Expense expense = Expense.builder()
                .user(currentUser)
                .season(season)
                .task(task)
                .category(request.getCategory())
                .amount(request.getAmount())
                .note(request.getNote())
                .itemName(itemName)
                .unitPrice(request.getUnitPrice() != null ? request.getUnitPrice() : request.getAmount())
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .totalCost(totalCost)
                .expenseDate(request.getExpenseDate())
                .createdAt(LocalDateTime.now())
                .build();

        Expense saved = expenseRepository.save(expense);

        // BR176: Step (7) - Return success (MSG 7 handled by controller)
        return toResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BR177: Query expense by ID - SELECT * FROM expense WHERE expense_id = [ID]
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * BR177: Query expense by ID for display on update screen.
     * Used by displayExpenseUpdateScreen(Expense expense) method.
     *
     * @param id the expense ID from datagrid selection
     * @return ExpenseResponse with all fields for BR178 display
     * @throws AppException with MSG 10 if not found
     */
    public ExpenseResponse getExpense(Integer id) {
        Expense expense = getExpenseForCurrentFarmer(id);
        return toResponse(expense);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BR180: UpdateExpense(Expense expense) - Update Expense with Full Validation
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * BR180: UpdateExpense method with parameter "expense" as an object class.
     * Validates all constraints before updating in database.
     *
     * @param id      the expense ID to update
     * @param request the expense update request containing all form fields
     * @return ExpenseResponse on success (MSG 7)
     * @throws AppException with MSG 9 on constraint violation
     */
    public ExpenseResponse UpdateExpense(Integer id, UpdateExpenseRequest request) {
        // BR179: ValidateDataFormat() - Input validation done via @Valid annotations

        Expense expense = getExpenseForCurrentFarmer(id);

        // Get target season (may be different if user changed season)
        Season targetSeason = getSeasonForCurrentFarmer(request.getSeasonId());

        // BR180: Validate target season is not closed/archived
        ensureSeasonOpenForExpenses(targetSeason);

        // BR180: Validate [Expense.season_id] is consistent with [Expense.plot_id]
        validateSeasonBelongsToPlot(targetSeason, request.getPlotId());

        // BR180: Validate [Expense.task_id] belongs to Season/Plot if provided
        Task task = null;
        if (request.getTaskId() != null) {
            task = validateTaskBelongsToSeason(request.getTaskId(), request.getSeasonId());
        }

        // BR179: Validate amount > 0
        validateAmount(request.getAmount());

        // Validate expense date within season dates
        validateExpenseDateWithinSeason(targetSeason, request.getExpenseDate());

        // Calculate totalCost for legacy compatibility
        BigDecimal totalCost = request.getAmount();
        if (request.getUnitPrice() != null && request.getQuantity() != null) {
            totalCost = request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        }

        // Determine itemName (use category if not provided)
        String itemName = request.getItemName();
        if (itemName == null || itemName.isBlank()) {
            itemName = request.getCategory() != null ? request.getCategory() : expense.getItemName();
        }

        // Update all fields
        expense.setSeason(targetSeason);
        expense.setTask(task);
        expense.setCategory(request.getCategory());
        expense.setAmount(request.getAmount());
        expense.setNote(request.getNote());
        expense.setItemName(itemName);
        expense.setUnitPrice(request.getUnitPrice() != null ? request.getUnitPrice() : request.getAmount());
        expense.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
        expense.setTotalCost(totalCost);
        expense.setExpenseDate(request.getExpenseDate());

        Expense saved = expenseRepository.save(expense);

        // BR180: Step (8) - Return success (MSG 7 handled by controller)
        return toResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BR182/BR183: DeleteExpense(Expense expense) - Delete with Constraint Check
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * BR182: DeleteExpense method called after confirmation dialog.
     * BR183: Validates constraints before deleting from database.
     *
     * @param id the expense ID to delete
     * @throws AppException with MSG 9 on constraint violation
     */
    public void DeleteExpense(Integer id) {
        Expense expense = getExpenseForCurrentFarmer(id);

        // BR183: Check constraints - season must not be closed
        ensureSeasonOpenForExpenses(expense.getSeason());

        expenseRepository.delete(expense);
        // BR183: Step (7) - Success (MSG 7 handled by controller)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BR185: SearchExpense(ExpenseSearchCriteria criteria) - Search with Criteria
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * BR185: SearchExpense method using ExpenseSearchCriteria.
     * Queries expense table based on given parameters.
     *
     * @param criteria the search criteria from form controls
     * @param page     page number (0-indexed)
     * @param size     page size
     * @return PageResponse with results (BR186) or empty with MSG 10 indicator
     *         (BR187)
     */
    public PageResponse<ExpenseResponse> SearchExpense(ExpenseSearchCriteria criteria, int page, int size) {
        User currentUser = getCurrentUser();

        // Get all expenses for this farmer's seasons
        List<Season> farmerSeasons = seasonRepository.findAllByFarmUserId(currentUser.getId());
        List<Integer> seasonIds = farmerSeasons.stream().map(Season::getId).toList();

        if (seasonIds.isEmpty()) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
            return PageResponse.of(new PageImpl<>(List.of(), pageable, 0), List.of());
        }

        // Get all expenses for these seasons
        List<Expense> allExpenses = new ArrayList<>();
        for (Integer seasonId : seasonIds) {
            allExpenses.addAll(expenseRepository.findAllBySeason_Id(seasonId));
        }

        // BR185: Apply search criteria filters
        List<ExpenseResponse> filtered = allExpenses.stream()
                .filter(expense -> {
                    // Filter by seasonId
                    if (criteria.getSeasonId() != null &&
                            !criteria.getSeasonId().equals(expense.getSeason().getId())) {
                        return false;
                    }
                    // Filter by plotId (season's plot)
                    if (criteria.getPlotId() != null &&
                            (expense.getSeason().getPlot() == null ||
                                    !criteria.getPlotId().equals(expense.getSeason().getPlot().getId()))) {
                        return false;
                    }
                    // Filter by taskId
                    if (criteria.getTaskId() != null &&
                            (expense.getTask() == null ||
                                    !criteria.getTaskId().equals(expense.getTask().getId()))) {
                        return false;
                    }
                    // Filter by category
                    if (criteria.getCategory() != null && !criteria.getCategory().isBlank() &&
                            (expense.getCategory() == null ||
                                    !expense.getCategory().equalsIgnoreCase(criteria.getCategory()))) {
                        return false;
                    }
                    // Filter by date range
                    LocalDate date = expense.getExpenseDate();
                    if (criteria.getFromDate() != null && date.isBefore(criteria.getFromDate())) {
                        return false;
                    }
                    if (criteria.getToDate() != null && date.isAfter(criteria.getToDate())) {
                        return false;
                    }
                    // Filter by amount range
                    BigDecimal amount = expense.getEffectiveAmount();
                    if (criteria.getMinAmount() != null && amount.compareTo(criteria.getMinAmount()) < 0) {
                        return false;
                    }
                    if (criteria.getMaxAmount() != null && amount.compareTo(criteria.getMaxAmount()) > 0) {
                        return false;
                    }
                    // Filter by keyword (itemName)
                    if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
                        String kw = criteria.getKeyword().toLowerCase();
                        if (expense.getItemName() == null ||
                                !expense.getItemName().toLowerCase().contains(kw)) {
                            return false;
                        }
                    }
                    return true;
                })
                .sorted((e1, e2) -> Integer.compare(
                        e2.getId() != null ? e2.getId() : 0,
                        e1.getId() != null ? e1.getId() : 0))
                .map(this::toResponse)
                .toList();

        // Paginate
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, filtered.size());
        List<ExpenseResponse> pageItems = fromIndex >= filtered.size() ? List.of()
                : filtered.subList(fromIndex, toIndex);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<ExpenseResponse> pageData = new PageImpl<>(pageItems, pageable, filtered.size());

        // BR186: Return results if available
        // BR187: Empty results indicate "Expense not found" (MSG 10 handled by
        // controller)
        return PageResponse.of(pageData, pageItems);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ADDITIONAL LIST METHODS (For compatibility with existing functionality)
    // ═══════════════════════════════════════════════════════════════════════════

    public PageResponse<ExpenseResponse> listExpensesForSeason(
            Integer seasonId,
            LocalDate from,
            LocalDate to,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            int page,
            int size) {
        Season season = getSeasonForCurrentFarmer(seasonId);

        List<Expense> all = expenseRepository.findAllBySeason_Id(season.getId());

        List<ExpenseResponse> items = all.stream()
                .filter(expense -> {
                    if (from == null && to == null) {
                        return true;
                    }
                    LocalDate date = expense.getExpenseDate();
                    boolean afterFrom = from == null || !date.isBefore(from);
                    boolean beforeTo = to == null || !date.isAfter(to);
                    return afterFrom && beforeTo;
                })
                .filter(expense -> {
                    BigDecimal total = expense.getEffectiveAmount();
                    boolean aboveMin = minAmount == null || total.compareTo(minAmount) >= 0;
                    boolean belowMax = maxAmount == null || total.compareTo(maxAmount) <= 0;
                    return aboveMin && belowMax;
                })
                .sorted((e1, e2) -> Integer.compare(
                        e2.getId() != null ? e2.getId() : 0,
                        e1.getId() != null ? e1.getId() : 0))
                .map(this::toResponse)
                .toList();

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, items.size());
        List<ExpenseResponse> pageItems = fromIndex >= items.size() ? List.of() : items.subList(fromIndex, toIndex);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<ExpenseResponse> pageData = new PageImpl<>(pageItems, pageable, items.size());

        return PageResponse.of(pageData, pageItems);
    }

    public PageResponse<ExpenseResponse> listAllFarmerExpenses(
            Integer seasonId,
            String q,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {

        User currentUser = getCurrentUser();
        Long userId = currentUser.getId();

        List<Expense> all;
        if (seasonId != null) {
            Season season = getSeasonForCurrentFarmer(seasonId);
            all = expenseRepository.findAllBySeason_Id(season.getId());
        } else if (q != null && !q.trim().isEmpty()) {
            all = expenseRepository.findAllByUser_IdAndItemNameContainingIgnoreCaseOrderByExpenseDateDesc(userId,
                    q.trim());
        } else {
            all = expenseRepository.findAllByUser_IdOrderByExpenseDateDesc(userId);
        }

        List<ExpenseResponse> items = all.stream()
                .filter(expense -> {
                    if (from == null && to == null) {
                        return true;
                    }
                    LocalDate date = expense.getExpenseDate();
                    boolean afterFrom = from == null || !date.isBefore(from);
                    boolean beforeTo = to == null || !date.isAfter(to);
                    return afterFrom && beforeTo;
                })
                .filter(expense -> {
                    if (seasonId == null || q == null || q.trim().isEmpty()) {
                        return true;
                    }
                    return expense.getItemName().toLowerCase().contains(q.toLowerCase().trim());
                })
                .sorted((e1, e2) -> {
                    int dateCompare = e2.getExpenseDate().compareTo(e1.getExpenseDate());
                    if (dateCompare != 0)
                        return dateCompare;
                    return Integer.compare(
                            e2.getId() != null ? e2.getId() : 0,
                            e1.getId() != null ? e1.getId() : 0);
                })
                .map(this::toResponse)
                .toList();

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, items.size());
        List<ExpenseResponse> pageItems = fromIndex >= items.size() ? List.of() : items.subList(fromIndex, toIndex);

        Pageable pageable = PageRequest.of(page, size, Sort.by("expenseDate").descending());
        Page<ExpenseResponse> pageData = new PageImpl<>(pageItems, pageable, items.size());

        return PageResponse.of(pageData, pageItems);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LEGACY WRAPPER METHODS (For backward compatibility)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Legacy createExpense method for backward compatibility.
     * Delegates to the new BR-compliant CreateExpense method.
     */
    public ExpenseResponse createExpense(Integer seasonId, CreateExpenseRequest request) {
        return CreateExpense(seasonId, request);
    }

    /**
     * Legacy updateExpense method for backward compatibility.
     */
    public ExpenseResponse updateExpense(Integer id, UpdateExpenseRequest request) {
        return UpdateExpense(id, request);
    }

    /**
     * Legacy deleteExpense method for backward compatibility.
     */
    public void deleteExpense(Integer id) {
        DeleteExpense(id);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BR176/BR180: CONSTRAINT VALIDATION METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * BR176/BR180: Validate that season belongs to the specified plot.
     * If not, throw MSG 9: "Your action is failed due to constraints in the
     * system."
     *
     * @param season the season to validate
     * @param plotId the expected plot ID
     * @throws AppException with MSG_9_CONSTRAINT_VIOLATION if mismatch
     */
    private void validateSeasonBelongsToPlot(Season season, Integer plotId) {
        if (plotId == null) {
            throw new AppException(ErrorCode.MSG_1_MANDATORY_FIELD_EMPTY);
        }
        if (season.getPlot() == null || !season.getPlot().getId().equals(plotId)) {
            // MSG 9: "Your action is failed due to constraints in the system."
            throw new AppException(ErrorCode.MSG_9_CONSTRAINT_VIOLATION);
        }
    }

    /**
     * BR176/BR180: Validate that task belongs to the specified season.
     * If not, throw MSG 9: "Your action is failed due to constraints in the
     * system."
     *
     * @param taskId   the task ID to validate
     * @param seasonId the expected season ID
     * @return the validated Task entity
     * @throws AppException with MSG_9_CONSTRAINT_VIOLATION if mismatch
     */
    private Task validateTaskBelongsToSeason(Integer taskId, Integer seasonId) {
        if (!taskRepository.existsByIdAndSeasonId(taskId, seasonId)) {
            // MSG 9: "Your action is failed due to constraints in the system."
            throw new AppException(ErrorCode.MSG_9_CONSTRAINT_VIOLATION);
        }
        return taskRepository.findByIdAndSeasonId(taskId, seasonId)
                .orElseThrow(() -> new AppException(ErrorCode.MSG_9_CONSTRAINT_VIOLATION));
    }

    /**
     * BR175/BR179: Validate that amount is greater than 0.
     *
     * @param amount the amount to validate
     * @throws AppException with MSG_4_INVALID_FORMAT if amount <= 0
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            // MSG 4: "Invalid data format. Please enter again."
            throw new AppException(ErrorCode.MSG_4_INVALID_FORMAT);
        }
    }

    /**
     * Ensure season is open for expense modifications.
     * BR176/BR180: Cannot modify expenses in closed/archived seasons.
     */
    private void ensureSeasonOpenForExpenses(Season season) {
        if (season == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }
        if (season.getStatus() == SeasonStatus.COMPLETED
                || season.getStatus() == SeasonStatus.CANCELLED
                || season.getStatus() == SeasonStatus.ARCHIVED) {
            throw new AppException(ErrorCode.EXPENSE_PERIOD_LOCKED);
        }
    }

    /**
     * Validate expense date is within season date range.
     */
    private void validateExpenseDateWithinSeason(Season season, LocalDate date) {
        LocalDate start = season.getStartDate();
        LocalDate end = season.getEndDate() != null ? season.getEndDate() : season.getPlannedHarvestDate();

        if (start == null || date.isBefore(start) || (end != null && date.isAfter(end))) {
            throw new AppException(ErrorCode.INVALID_SEASON_DATES);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    private Expense getExpenseForCurrentFarmer(Integer id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MSG_10_EXPENSE_NOT_FOUND));

        Season season = expense.getSeason();
        if (season == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }
        farmAccessService.assertCurrentUserCanAccessSeason(season);
        return expense;
    }

    private Season getSeasonForCurrentFarmer(Integer id) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MSG_9_CONSTRAINT_VIOLATION));
        farmAccessService.assertCurrentUserCanAccessSeason(season);
        return season;
    }

    private User getCurrentUser() {
        return farmAccessService.getCurrentUser();
    }

    /**
     * Convert Expense entity to ExpenseResponse DTO.
     * Includes all fields required by BR177/BR178 for display screens.
     */
    private ExpenseResponse toResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                // Season info
                .seasonId(expense.getSeason() != null ? expense.getSeason().getId() : null)
                .seasonName(expense.getSeason() != null ? expense.getSeason().getSeasonName() : null)
                // Plot info (BR176/BR180)
                .plotId(expense.getSeason() != null && expense.getSeason().getPlot() != null
                        ? expense.getSeason().getPlot().getId()
                        : null)
                .plotName(expense.getSeason() != null && expense.getSeason().getPlot() != null
                        ? expense.getSeason().getPlot().getPlotName()
                        : null)
                // Task info (BR176/BR180)
                .taskId(expense.getTask() != null ? expense.getTask().getId() : null)
                .taskTitle(expense.getTask() != null ? expense.getTask().getTitle() : null)
                // User info
                .userName(expense.getUser() != null ? expense.getUser().getUsername() : null)
                // BR fields
                .category(expense.getCategory())
                .amount(expense.getEffectiveAmount())
                .note(expense.getNote())
                .expenseDate(expense.getExpenseDate())
                .createdAt(expense.getCreatedAt())
                // Legacy fields
                .itemName(expense.getItemName())
                .unitPrice(expense.getUnitPrice())
                .quantity(expense.getQuantity())
                .totalCost(expense.getTotalCost())
                .build();
    }
}
