package org.example.QuanLyMuaVu.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.DTO.Request.CreateExpenseRequest;
import org.example.QuanLyMuaVu.DTO.Request.ExpenseSearchCriteria;
import org.example.QuanLyMuaVu.DTO.Request.UpdateExpenseRequest;
import org.example.QuanLyMuaVu.DTO.Response.BudgetTrackerResponse;
import org.example.QuanLyMuaVu.DTO.Response.ExpenseCategoryAnalyticsResponse;
import org.example.QuanLyMuaVu.DTO.Response.ExpenseResponse;
import org.example.QuanLyMuaVu.DTO.Response.ExpenseTaskAnalyticsResponse;
import org.example.QuanLyMuaVu.DTO.Response.ExpenseTimeSeriesResponse;
import org.example.QuanLyMuaVu.DTO.Response.ExpenseVendorAnalyticsResponse;
import org.example.QuanLyMuaVu.Entity.Expense;
import org.example.QuanLyMuaVu.Entity.Farm;
import org.example.QuanLyMuaVu.Entity.Plot;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Entity.Supplier;
import org.example.QuanLyMuaVu.Entity.Task;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Enums.ExpenseTimeGranularity;
import org.example.QuanLyMuaVu.Enums.PaymentStatus;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.ExpenseRepository;
import org.example.QuanLyMuaVu.Repository.SeasonRepository;
import org.example.QuanLyMuaVu.Repository.SupplierRepository;
import org.example.QuanLyMuaVu.Repository.TaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * BR174-BR187: Season Expense Service
 * Handles all expense CRUD operations with BR-compliant validations.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class SeasonExpenseService {

    private static final long MAX_ATTACHMENT_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_ATTACHMENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png");

    ExpenseRepository expenseRepository;
    SeasonRepository seasonRepository;
    TaskRepository taskRepository;
    SupplierRepository supplierRepository;
    FarmAccessService farmAccessService;

    @NonFinal
    @Value("${app.expense.attachments.dir:uploads/expenses}")
    String attachmentsDir;

    // BR176: CreateExpense(Expense expense) - Create Expense with Full Validation
    public ExpenseResponse CreateExpense(Integer seasonId, CreateExpenseRequest request) {
        Season season = getSeasonForCurrentFarmer(seasonId);
        ensureSeasonOpenForExpenses(season);

        validateSeasonBelongsToPlot(season, request.getPlotId());

        Task task = null;
        if (request.getTaskId() != null) {
            task = validateTaskBelongsToSeason(request.getTaskId(), seasonId);
        }

        Supplier supplier = null;
        if (request.getVendorId() != null) {
            supplier = supplierRepository.findById(request.getVendorId())
                    .orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_FOUND));
        }

        validateAmount(request.getAmount());
        validateExpenseDateWithinSeason(season, request.getExpenseDate());

        User currentUser = getCurrentUser();

        BigDecimal unitPrice = request.getUnitPrice() != null ? request.getUnitPrice() : request.getAmount();
        Integer quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        BigDecimal totalCost = request.getUnitPrice() != null && request.getQuantity() != null
                ? unitPrice.multiply(BigDecimal.valueOf(quantity))
                : request.getAmount();

        String itemName = request.getItemName();
        if (itemName == null || itemName.isBlank()) {
            itemName = request.getCategory() != null ? request.getCategory() : "Expense";
        }

        PaymentStatus status = request.getPaymentStatus() != null
                ? request.getPaymentStatus()
                : PaymentStatus.PENDING;

        Expense expense = Expense.builder()
                .user(currentUser)
                .season(season)
                .task(task)
                .supplier(supplier)
                .category(request.getCategory())
                .amount(request.getAmount())
                .paymentStatus(status)
                .note(request.getNote())
                .itemName(itemName)
                .unitPrice(unitPrice)
                .quantity(quantity)
                .totalCost(totalCost)
                .expenseDate(request.getExpenseDate())
                .createdAt(LocalDateTime.now())
                .build();

        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }

    // BR177: Query expense by ID
    public ExpenseResponse getExpense(Integer id) {
        Expense expense = getExpenseForCurrentFarmer(id);
        return toResponse(expense);
    }

    // BR180: UpdateExpense(Expense expense) - Update Expense with Full Validation
    public ExpenseResponse UpdateExpense(Integer id, UpdateExpenseRequest request) {
        Expense expense = getExpenseForCurrentFarmer(id);

        Season targetSeason = getSeasonForCurrentFarmer(request.getSeasonId());
        ensureSeasonOpenForExpenses(targetSeason);

        validateSeasonBelongsToPlot(targetSeason, request.getPlotId());

        Task task = null;
        if (request.getTaskId() != null) {
            task = validateTaskBelongsToSeason(request.getTaskId(), request.getSeasonId());
        }

        Supplier supplier = null;
        if (request.getVendorId() != null) {
            supplier = supplierRepository.findById(request.getVendorId())
                    .orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_FOUND));
        }

        validateAmount(request.getAmount());
        validateExpenseDateWithinSeason(targetSeason, request.getExpenseDate());

        BigDecimal unitPrice = request.getUnitPrice() != null ? request.getUnitPrice() : request.getAmount();
        Integer quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        BigDecimal totalCost = request.getUnitPrice() != null && request.getQuantity() != null
                ? unitPrice.multiply(BigDecimal.valueOf(quantity))
                : request.getAmount();

        String itemName = request.getItemName();
        if (itemName == null || itemName.isBlank()) {
            itemName = request.getCategory() != null ? request.getCategory() : expense.getItemName();
        }

        expense.setSeason(targetSeason);
        expense.setTask(task);
        expense.setSupplier(supplier);
        expense.setCategory(request.getCategory());
        expense.setAmount(request.getAmount());
        PaymentStatus status = request.getPaymentStatus() != null
                ? request.getPaymentStatus()
                : PaymentStatus.PENDING;
        expense.setPaymentStatus(status);
        expense.setNote(request.getNote());
        expense.setItemName(itemName);
        expense.setUnitPrice(unitPrice);
        expense.setQuantity(quantity);
        expense.setTotalCost(totalCost);
        expense.setExpenseDate(request.getExpenseDate());

        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }

    // BR182/BR183: DeleteExpense(Expense expense) - Soft delete
    public void DeleteExpense(Integer id) {
        Expense expense = getExpenseForCurrentFarmer(id);
        ensureSeasonOpenForExpenses(expense.getSeason());
        expense.setDeletedAt(LocalDateTime.now());
        expenseRepository.save(expense);
    }

    // BR185: SearchExpense(ExpenseSearchCriteria criteria) - Search with Criteria
    public PageResponse<ExpenseResponse> SearchExpense(ExpenseSearchCriteria criteria, int page, int size) {
        return listExpenses(criteria, page, size);
    }

    public PageResponse<ExpenseResponse> listExpensesForSeason(
            Integer seasonId,
            LocalDate from,
            LocalDate to,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            int page,
            int size) {
        ExpenseSearchCriteria criteria = ExpenseSearchCriteria.builder()
                .seasonId(seasonId)
                .fromDate(from)
                .toDate(to)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .build();
        return listExpenses(criteria, page, size);
    }

    public PageResponse<ExpenseResponse> listAllFarmerExpenses(
            Integer seasonId,
            String q,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {
        ExpenseSearchCriteria criteria = ExpenseSearchCriteria.builder()
                .seasonId(seasonId)
                .q(q)
                .fromDate(from)
                .toDate(to)
                .build();
        return listExpenses(criteria, page, size);
    }

    public BudgetTrackerResponse getBudgetTracker(Integer seasonId) {
        Season season = getSeasonForCurrentFarmer(seasonId);
        BigDecimal budgetAmount = season.getBudgetAmount();

        BigDecimal total = expenseRepository.sumTotalCostBySeasonId(seasonId);
        BigDecimal paid = expenseRepository.sumEffectiveAmountBySeasonIdAndStatus(seasonId, PaymentStatus.PAID);
        BigDecimal pending = expenseRepository.sumEffectiveAmountBySeasonIdAndStatus(seasonId, PaymentStatus.PENDING);
        BigDecimal unpaid = expenseRepository.sumEffectiveAmountBySeasonIdAndStatus(seasonId, PaymentStatus.UNPAID);
        BigDecimal unpaidTotal = pending.add(unpaid);

        BigDecimal usagePercent = null;
        BigDecimal remaining = null;
        if (budgetAmount != null && budgetAmount.compareTo(BigDecimal.ZERO) > 0) {
            usagePercent = total.multiply(BigDecimal.valueOf(100))
                    .divide(budgetAmount, 2, RoundingMode.HALF_UP);
            remaining = budgetAmount.subtract(total);
        }

        long totalCount = expenseRepository.countBySeasonId(seasonId);
        long withAttachment = expenseRepository.countBySeasonIdWithAttachment(seasonId);
        Double receiptCoverage = totalCount == 0 ? null : (double) withAttachment / (double) totalCount;

        return BudgetTrackerResponse.builder()
                .budgetAmount(budgetAmount)
                .total(total)
                .paid(paid)
                .unpaid(unpaidTotal)
                .usagePercent(usagePercent)
                .remaining(remaining)
                .receiptCoverage(receiptCoverage)
                .build();
    }

    public List<ExpenseCategoryAnalyticsResponse> analyticsByCategory(ExpenseSearchCriteria criteria) {
        List<Expense> expenses = expenseRepository.findAll(buildSpecification(criteria));
        Map<String, List<Expense>> grouped = expenses.stream()
                .collect(Collectors.groupingBy(expense -> {
                    String category = expense.getCategory();
                    return category == null || category.isBlank() ? "Uncategorized" : category;
                }));

        return grouped.entrySet().stream()
                .map(entry -> ExpenseCategoryAnalyticsResponse.builder()
                        .category(entry.getKey())
                        .totalAmount(sumAmounts(entry.getValue()))
                        .count((long) entry.getValue().size())
                        .build())
                .sorted(Comparator.comparing(ExpenseCategoryAnalyticsResponse::getTotalAmount).reversed())
                .toList();
    }

    public List<ExpenseTaskAnalyticsResponse> analyticsByTask(ExpenseSearchCriteria criteria) {
        List<Expense> expenses = expenseRepository.findAll(buildSpecification(criteria));
        Map<Task, List<Expense>> grouped = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getTask));

        return grouped.entrySet().stream()
                .map(entry -> {
                    Task task = entry.getKey();
                    return ExpenseTaskAnalyticsResponse.builder()
                            .taskId(task != null ? task.getId() : null)
                            .taskTitle(task != null ? task.getTitle() : "Unassigned")
                            .totalAmount(sumAmounts(entry.getValue()))
                            .count((long) entry.getValue().size())
                            .build();
                })
                .sorted(Comparator.comparing(ExpenseTaskAnalyticsResponse::getTotalAmount).reversed())
                .toList();
    }

    public List<ExpenseVendorAnalyticsResponse> analyticsByVendor(ExpenseSearchCriteria criteria) {
        List<Expense> expenses = expenseRepository.findAll(buildSpecification(criteria));
        Map<Supplier, List<Expense>> grouped = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getSupplier));

        return grouped.entrySet().stream()
                .map(entry -> {
                    Supplier supplier = entry.getKey();
                    return ExpenseVendorAnalyticsResponse.builder()
                            .vendorId(supplier != null ? supplier.getId() : null)
                            .vendorName(supplier != null ? supplier.getName() : "Unassigned")
                            .totalAmount(sumAmounts(entry.getValue()))
                            .count((long) entry.getValue().size())
                            .build();
                })
                .sorted(Comparator.comparing(ExpenseVendorAnalyticsResponse::getTotalAmount).reversed())
                .toList();
    }

    public List<ExpenseTimeSeriesResponse> analyticsTimeSeries(
            ExpenseSearchCriteria criteria,
            ExpenseTimeGranularity granularity) {
        List<Expense> expenses = expenseRepository.findAll(buildSpecification(criteria));
        Map<LocalDate, List<Expense>> grouped = new TreeMap<>();

        for (Expense expense : expenses) {
            LocalDate date = expense.getExpenseDate();
            LocalDate key = resolveTimeKey(date, granularity);
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(expense);
        }

        return grouped.entrySet().stream()
                .map(entry -> ExpenseTimeSeriesResponse.builder()
                        .periodStart(entry.getKey())
                        .label(formatTimeLabel(entry.getKey(), granularity))
                        .totalAmount(sumAmounts(entry.getValue()))
                        .count((long) entry.getValue().size())
                        .build())
                .toList();
    }

    public byte[] exportCsv(ExpenseSearchCriteria criteria) {
        List<Expense> expenses = expenseRepository.findAll(
                buildSpecification(criteria),
                Sort.by(Sort.Direction.DESC, "expenseDate", "id"));

        StringBuilder sb = new StringBuilder();
        sb.append("Date,Category,Description,Season,Task,Vendor,Amount,PaymentStatus,Notes\n");
        for (Expense expense : expenses) {
            sb.append(escapeCsv(expense.getExpenseDate() != null ? expense.getExpenseDate().toString() : ""))
                    .append(',')
                    .append(escapeCsv(expense.getCategory()))
                    .append(',')
                    .append(escapeCsv(expense.getItemName()))
                    .append(',')
                    .append(escapeCsv(expense.getSeason() != null ? expense.getSeason().getSeasonName() : ""))
                    .append(',')
                    .append(escapeCsv(expense.getTask() != null ? expense.getTask().getTitle() : ""))
                    .append(',')
                    .append(escapeCsv(expense.getSupplier() != null ? expense.getSupplier().getName() : ""))
                    .append(',')
                    .append(escapeCsv(expense.getEffectiveAmount().toPlainString()))
                    .append(',')
                    .append(escapeCsv(expense.getPaymentStatus() != null ? expense.getPaymentStatus().name() : ""))
                    .append(',')
                    .append(escapeCsv(expense.getNote()))
                    .append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public ExpenseResponse uploadAttachment(Integer expenseId, MultipartFile file) {
        Expense expense = getExpenseForCurrentFarmer(expenseId);
        ensureSeasonOpenForExpenses(expense.getSeason());
        validateAttachment(file);

        Path baseDir = getAttachmentBasePath();
        Path expenseDir = baseDir.resolve(String.valueOf(expenseId));

        try {
            Files.createDirectories(expenseDir);

            String originalName = StringUtils.cleanPath(
                    Objects.toString(file.getOriginalFilename(), "attachment"));
            String sanitizedName = originalName.replaceAll("\\s+", "_");
            String fileName = System.currentTimeMillis() + "-" + sanitizedName;
            Path targetPath = expenseDir.resolve(fileName).normalize();

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            Path relativePath = baseDir.relativize(targetPath);
            expense.setAttachmentPath(relativePath.toString().replace('\\', '/'));
            expense.setAttachmentName(originalName);
            expense.setAttachmentMime(file.getContentType());
            expense.setAttachmentUrl("/api/v1/expenses/" + expenseId + "/attachment");
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }

    public AttachmentDownload downloadAttachment(Integer expenseId) {
        Expense expense = getExpenseForCurrentFarmer(expenseId);
        if (expense.getAttachmentPath() == null) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        Path filePath = getAttachmentBasePath().resolve(expense.getAttachmentPath()).normalize();
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            String contentType = expense.getAttachmentMime();
            if (contentType == null || contentType.isBlank()) {
                contentType = Files.probeContentType(filePath);
            }
            long size = Files.size(filePath);
            return new AttachmentDownload(resource, expense.getAttachmentName(), contentType, size);
        } catch (IOException ex) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    // Legacy wrapper methods (backward compatibility)
    public ExpenseResponse createExpense(Integer seasonId, CreateExpenseRequest request) {
        return CreateExpense(seasonId, request);
    }

    public ExpenseResponse updateExpense(Integer id, UpdateExpenseRequest request) {
        return UpdateExpense(id, request);
    }

    public void deleteExpense(Integer id) {
        DeleteExpense(id);
    }

    // Constraint validation methods
    private void validateSeasonBelongsToPlot(Season season, Integer plotId) {
        if (plotId == null) {
            return;
        }
        if (season.getPlot() == null || !season.getPlot().getId().equals(plotId)) {
            throw new AppException(ErrorCode.MSG_9_CONSTRAINT_VIOLATION);
        }
    }

    private Task validateTaskBelongsToSeason(Integer taskId, Integer seasonId) {
        if (!taskRepository.existsByIdAndSeasonId(taskId, seasonId)) {
            throw new AppException(ErrorCode.MSG_9_CONSTRAINT_VIOLATION);
        }
        return taskRepository.findByIdAndSeasonId(taskId, seasonId)
                .orElseThrow(() -> new AppException(ErrorCode.MSG_9_CONSTRAINT_VIOLATION));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.MSG_4_INVALID_FORMAT);
        }
    }

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

    private void validateExpenseDateWithinSeason(Season season, LocalDate date) {
        LocalDate start = season.getStartDate();
        LocalDate end = season.getEndDate() != null ? season.getEndDate() : season.getPlannedHarvestDate();

        if (start == null || date.isBefore(start) || (end != null && date.isAfter(end))) {
            throw new AppException(ErrorCode.INVALID_SEASON_DATES);
        }
    }

    // Helper methods
    private Expense getExpenseForCurrentFarmer(Integer id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MSG_10_EXPENSE_NOT_FOUND));
        if (expense.getDeletedAt() != null) {
            throw new AppException(ErrorCode.MSG_10_EXPENSE_NOT_FOUND);
        }
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

    private Specification<Expense> buildSpecification(ExpenseSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isNull(root.get("deletedAt")));

            Join<Expense, Season> seasonJoin = root.join("season", JoinType.INNER);
            Join<Season, Plot> plotJoin = seasonJoin.join("plot", JoinType.LEFT);
            Join<Plot, Farm> farmJoin = plotJoin.join("farm", JoinType.LEFT);

            List<Integer> accessibleFarmIds = farmAccessService.getAccessibleFarmIdsForCurrentUser();
            Long currentUserId = farmAccessService.getCurrentUser().getId();

            Predicate farmOwned = accessibleFarmIds.isEmpty()
                    ? cb.disjunction()
                    : farmJoin.get("id").in(accessibleFarmIds);
            Predicate plotOwned = cb.equal(plotJoin.get("user").get("id"), currentUserId);
            predicates.add(cb.or(farmOwned, plotOwned));

            if (criteria != null) {
                if (criteria.getSeasonId() != null) {
                    predicates.add(cb.equal(seasonJoin.get("id"), criteria.getSeasonId()));
                }
                if (criteria.getPlotId() != null) {
                    predicates.add(cb.equal(plotJoin.get("id"), criteria.getPlotId()));
                }
                if (criteria.getCropId() != null) {
                    predicates.add(cb.equal(seasonJoin.get("crop").get("id"), criteria.getCropId()));
                }
                if (criteria.getTaskId() != null) {
                    predicates.add(cb.equal(root.get("task").get("id"), criteria.getTaskId()));
                }
                if (criteria.getVendorId() != null) {
                    predicates.add(cb.equal(root.get("supplier").get("id"), criteria.getVendorId()));
                }
                if (criteria.getCategory() != null && !criteria.getCategory().isBlank()) {
                    predicates.add(cb.equal(
                            cb.lower(root.get("category")),
                            criteria.getCategory().toLowerCase(Locale.ROOT)));
                }
                if (criteria.getPaymentStatus() != null) {
                    predicates.add(cb.equal(root.get("paymentStatus"), criteria.getPaymentStatus()));
                }
                if (criteria.getFromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("expenseDate"), criteria.getFromDate()));
                }
                if (criteria.getToDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("expenseDate"), criteria.getToDate()));
                }

                Expression<BigDecimal> effectiveAmount = cb.coalesce(root.get("amount"), root.get("totalCost"));
                if (criteria.getMinAmount() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(effectiveAmount, criteria.getMinAmount()));
                }
                if (criteria.getMaxAmount() != null) {
                    predicates.add(cb.lessThanOrEqualTo(effectiveAmount, criteria.getMaxAmount()));
                }

                String keyword = resolveKeyword(criteria.getKeyword(), criteria.getQ());
                if (keyword != null && !keyword.isBlank()) {
                    String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                    Join<Expense, Supplier> supplierJoin = root.join("supplier", JoinType.LEFT);
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("itemName")), pattern),
                            cb.like(cb.lower(root.get("note")), pattern),
                            cb.like(cb.lower(root.get("category")), pattern),
                            cb.like(cb.lower(supplierJoin.get("name")), pattern)));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String resolveKeyword(String keyword, String q) {
        if (q != null && !q.isBlank()) {
            return q;
        }
        return keyword;
    }

    private PageResponse<ExpenseResponse> listExpenses(ExpenseSearchCriteria criteria, int page, int size) {
        if (criteria != null && criteria.getSeasonId() != null) {
            getSeasonForCurrentFarmer(criteria.getSeasonId());
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "expenseDate", "id"));
        Page<Expense> pageData = expenseRepository.findAll(buildSpecification(criteria), pageable);

        List<ExpenseResponse> items = pageData.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.of(pageData, items);
    }

    private LocalDate resolveTimeKey(LocalDate date, ExpenseTimeGranularity granularity) {
        if (granularity == null) {
            return date;
        }
        switch (granularity) {
            case WEEK:
                return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH:
                return date.withDayOfMonth(1);
            case DAY:
            default:
                return date;
        }
    }

    private String formatTimeLabel(LocalDate date, ExpenseTimeGranularity granularity) {
        if (granularity == null) {
            return date.toString();
        }
        switch (granularity) {
            case WEEK:
                WeekFields wf = WeekFields.ISO;
                int weekYear = date.get(wf.weekBasedYear());
                int week = date.get(wf.weekOfWeekBasedYear());
                return String.format("%d-W%02d", weekYear, week);
            case MONTH:
                YearMonth ym = YearMonth.from(date);
                return ym.toString();
            case DAY:
            default:
                return date.toString();
        }
    }

    private BigDecimal sumAmounts(List<Expense> expenses) {
        return expenses.stream()
                .map(Expense::getEffectiveAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    private Path getAttachmentBasePath() {
        return Paths.get(attachmentsDir).toAbsolutePath().normalize();
    }

    private void validateAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.MSG_1_MANDATORY_FIELD_EMPTY);
        }
        if (file.getSize() > MAX_ATTACHMENT_BYTES) {
            throw new AppException(ErrorCode.MSG_4_INVALID_FORMAT);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_ATTACHMENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new AppException(ErrorCode.MSG_4_INVALID_FORMAT);
        }
    }

    /**
     * Convert Expense entity to ExpenseResponse DTO.
     */
    private ExpenseResponse toResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .seasonId(expense.getSeason() != null ? expense.getSeason().getId() : null)
                .seasonName(expense.getSeason() != null ? expense.getSeason().getSeasonName() : null)
                .plotId(expense.getSeason() != null && expense.getSeason().getPlot() != null
                        ? expense.getSeason().getPlot().getId()
                        : null)
                .plotName(expense.getSeason() != null && expense.getSeason().getPlot() != null
                        ? expense.getSeason().getPlot().getPlotName()
                        : null)
                .taskId(expense.getTask() != null ? expense.getTask().getId() : null)
                .taskTitle(expense.getTask() != null ? expense.getTask().getTitle() : null)
                .userName(expense.getUser() != null ? expense.getUser().getUsername() : null)
                .category(expense.getCategory())
                .amount(expense.getEffectiveAmount())
                .paymentStatus(expense.getPaymentStatus())
                .vendorId(expense.getSupplier() != null ? expense.getSupplier().getId() : null)
                .vendorName(expense.getSupplier() != null ? expense.getSupplier().getName() : null)
                .note(expense.getNote())
                .expenseDate(expense.getExpenseDate())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .attachmentUrl(expense.getAttachmentUrl())
                .attachmentName(expense.getAttachmentName())
                .attachmentMime(expense.getAttachmentMime())
                .itemName(expense.getItemName())
                .unitPrice(expense.getUnitPrice())
                .quantity(expense.getQuantity())
                .totalCost(expense.getTotalCost())
                .build();
    }

    public static class AttachmentDownload {
        private final Resource resource;
        private final String filename;
        private final String contentType;
        private final long size;

        public AttachmentDownload(Resource resource, String filename, String contentType, long size) {
            this.resource = resource;
            this.filename = filename;
            this.contentType = contentType;
            this.size = size;
        }

        public Resource getResource() {
            return resource;
        }

        public String getFilename() {
            return filename;
        }

        public String getContentType() {
            return contentType;
        }

        public long getSize() {
            return size;
        }
    }
}
