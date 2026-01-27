package org.example.QuanLyMuaVu.Controller.Admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.DTO.Request.CreateExpenseRequest;
import org.example.QuanLyMuaVu.DTO.Request.ExpenseSearchCriteria;
import org.example.QuanLyMuaVu.DTO.Request.UpdateExpenseRequest;
import org.example.QuanLyMuaVu.DTO.Response.BudgetTrackerResponse;
import org.example.QuanLyMuaVu.DTO.Response.ExpenseCategoryAnalyticsResponse;
import org.example.QuanLyMuaVu.DTO.Response.ExpenseListResponse;
import org.example.QuanLyMuaVu.DTO.Response.ExpenseResponse;
import org.example.QuanLyMuaVu.DTO.Response.ExpenseTaskAnalyticsResponse;
import org.example.QuanLyMuaVu.DTO.Response.ExpenseTimeSeriesResponse;
import org.example.QuanLyMuaVu.DTO.Response.ExpenseVendorAnalyticsResponse;
import org.example.QuanLyMuaVu.Enums.ExpenseTimeGranularity;
import org.example.QuanLyMuaVu.Enums.PaymentStatus;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Service.SeasonExpenseService;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Admin endpoints for Expense Management.
 * Provides system-wide access to expenses with the same filters and analytics.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin Expenses", description = "Admin endpoints for system-wide expense management")
public class AdminExpenseController {

    SeasonExpenseService seasonExpenseService;

    @Operation(summary = "Create expense for season (Admin)")
    @PostMapping("/seasons/{seasonId}/expenses")
    public ApiResponse<ExpenseResponse> createExpense(
            @Parameter(description = "Season ID from path") @PathVariable Integer seasonId,
            @Parameter(description = "Expense data from form fields") @Valid @RequestBody CreateExpenseRequest request) {

        ExpenseResponse response = seasonExpenseService.CreateExpense(seasonId, request);
        return ApiResponse.success(ErrorCode.MSG_7_SAVE_SUCCESS.getMessage(), response);
    }

    @Operation(summary = "Get expense by ID (Admin)")
    @GetMapping("/expenses/{id}")
    public ApiResponse<ExpenseResponse> getExpense(
            @Parameter(description = "Expense ID") @PathVariable Integer id) {
        return ApiResponse.success(seasonExpenseService.getExpense(id));
    }

    @Operation(summary = "Update expense (Admin)")
    @PutMapping("/expenses/{id}")
    public ApiResponse<ExpenseResponse> updateExpense(
            @Parameter(description = "Expense ID to update") @PathVariable Integer id,
            @Parameter(description = "Updated expense data from form fields") @Valid @RequestBody UpdateExpenseRequest request) {

        ExpenseResponse response = seasonExpenseService.UpdateExpense(id, request);
        return ApiResponse.success(ErrorCode.MSG_7_SAVE_SUCCESS.getMessage(), response);
    }

    @Operation(summary = "Delete expense (Admin)")
    @DeleteMapping("/expenses/{id}")
    public ApiResponse<Void> deleteExpense(
            @Parameter(description = "Expense ID to delete") @PathVariable Integer id) {
        seasonExpenseService.DeleteExpense(id);
        return ApiResponse.success(ErrorCode.MSG_7_SAVE_SUCCESS.getMessage(), null);
    }

    @Operation(summary = "Get delete confirmation message (Admin)")
    @GetMapping("/expenses/{id}/delete-confirmation")
    public ApiResponse<String> getDeleteConfirmation(
            @Parameter(description = "Expense ID to delete") @PathVariable Integer id) {
        return ApiResponse.success(ErrorCode.MSG_11_CONFIRMATION.getMessage(),
                ErrorCode.MSG_11_CONFIRMATION.getMessage());
    }

    @Operation(summary = "List expenses (Admin)")
    @GetMapping("/expenses")
    public ApiResponse<ExpenseListResponse> listExpenses(
            @RequestParam(value = "seasonId", required = false) Integer seasonId,
            @RequestParam(value = "plotId", required = false) Integer plotId,
            @RequestParam(value = "cropId", required = false) Integer cropId,
            @RequestParam(value = "taskId", required = false) Integer taskId,
            @RequestParam(value = "vendorId", required = false) Integer vendorId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "paymentStatus", required = false) PaymentStatus paymentStatus,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
            @RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        ExpenseSearchCriteria criteria = ExpenseSearchCriteria.builder()
                .seasonId(seasonId)
                .plotId(plotId)
                .cropId(cropId)
                .taskId(taskId)
                .vendorId(vendorId)
                .category(category)
                .paymentStatus(paymentStatus)
                .fromDate(fromDate)
                .toDate(toDate)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .q(q)
                .build();

        PageResponse<ExpenseResponse> result = seasonExpenseService.SearchExpense(criteria, page, size);
        return ApiResponse.success(ExpenseListResponse.from(result));
    }

    @Operation(summary = "List expenses for a season (Admin)")
    @GetMapping("/seasons/{seasonId}/expenses")
    public ApiResponse<ExpenseListResponse> listExpensesForSeason(
            @PathVariable Integer seasonId,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
            @RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "paymentStatus", required = false) PaymentStatus paymentStatus,
            @RequestParam(value = "vendorId", required = false) Integer vendorId,
            @RequestParam(value = "taskId", required = false) Integer taskId,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        ExpenseSearchCriteria criteria = ExpenseSearchCriteria.builder()
                .seasonId(seasonId)
                .fromDate(from)
                .toDate(to)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .category(category)
                .paymentStatus(paymentStatus)
                .vendorId(vendorId)
                .taskId(taskId)
                .q(q)
                .build();

        PageResponse<ExpenseResponse> result = seasonExpenseService.SearchExpense(criteria, page, size);
        return ApiResponse.success(ExpenseListResponse.from(result));
    }

    @Operation(summary = "Budget tracker for season (Admin)")
    @GetMapping("/seasons/{seasonId}/budget-tracker")
    public ApiResponse<BudgetTrackerResponse> budgetTracker(@PathVariable Integer seasonId) {
        return ApiResponse.success(seasonExpenseService.getBudgetTracker(seasonId));
    }

    @Operation(summary = "Expense analytics by category (Admin)")
    @GetMapping("/expenses/analytics/by-category")
    public ApiResponse<List<ExpenseCategoryAnalyticsResponse>> analyticsByCategory(
            @RequestParam(value = "seasonId", required = false) Integer seasonId,
            @RequestParam(value = "plotId", required = false) Integer plotId,
            @RequestParam(value = "cropId", required = false) Integer cropId,
            @RequestParam(value = "taskId", required = false) Integer taskId,
            @RequestParam(value = "vendorId", required = false) Integer vendorId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "paymentStatus", required = false) PaymentStatus paymentStatus,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "q", required = false) String q) {

        ExpenseSearchCriteria criteria = ExpenseSearchCriteria.builder()
                .seasonId(seasonId)
                .plotId(plotId)
                .cropId(cropId)
                .taskId(taskId)
                .vendorId(vendorId)
                .category(category)
                .paymentStatus(paymentStatus)
                .fromDate(fromDate)
                .toDate(toDate)
                .q(q)
                .build();

        return ApiResponse.success(seasonExpenseService.analyticsByCategory(criteria));
    }

    @Operation(summary = "Expense analytics by task (Admin)")
    @GetMapping("/expenses/analytics/by-task")
    public ApiResponse<List<ExpenseTaskAnalyticsResponse>> analyticsByTask(
            @RequestParam(value = "seasonId", required = false) Integer seasonId,
            @RequestParam(value = "plotId", required = false) Integer plotId,
            @RequestParam(value = "cropId", required = false) Integer cropId,
            @RequestParam(value = "taskId", required = false) Integer taskId,
            @RequestParam(value = "vendorId", required = false) Integer vendorId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "paymentStatus", required = false) PaymentStatus paymentStatus,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "q", required = false) String q) {

        ExpenseSearchCriteria criteria = ExpenseSearchCriteria.builder()
                .seasonId(seasonId)
                .plotId(plotId)
                .cropId(cropId)
                .taskId(taskId)
                .vendorId(vendorId)
                .category(category)
                .paymentStatus(paymentStatus)
                .fromDate(fromDate)
                .toDate(toDate)
                .q(q)
                .build();

        return ApiResponse.success(seasonExpenseService.analyticsByTask(criteria));
    }

    @Operation(summary = "Expense analytics by vendor (Admin)")
    @GetMapping("/expenses/analytics/by-vendor")
    public ApiResponse<List<ExpenseVendorAnalyticsResponse>> analyticsByVendor(
            @RequestParam(value = "seasonId", required = false) Integer seasonId,
            @RequestParam(value = "plotId", required = false) Integer plotId,
            @RequestParam(value = "cropId", required = false) Integer cropId,
            @RequestParam(value = "taskId", required = false) Integer taskId,
            @RequestParam(value = "vendorId", required = false) Integer vendorId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "paymentStatus", required = false) PaymentStatus paymentStatus,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "q", required = false) String q) {

        ExpenseSearchCriteria criteria = ExpenseSearchCriteria.builder()
                .seasonId(seasonId)
                .plotId(plotId)
                .cropId(cropId)
                .taskId(taskId)
                .vendorId(vendorId)
                .category(category)
                .paymentStatus(paymentStatus)
                .fromDate(fromDate)
                .toDate(toDate)
                .q(q)
                .build();

        return ApiResponse.success(seasonExpenseService.analyticsByVendor(criteria));
    }

    @Operation(summary = "Expense analytics time series (Admin)")
    @GetMapping("/expenses/analytics/timeseries")
    public ApiResponse<List<ExpenseTimeSeriesResponse>> analyticsTimeSeries(
            @RequestParam(value = "seasonId", required = false) Integer seasonId,
            @RequestParam(value = "plotId", required = false) Integer plotId,
            @RequestParam(value = "cropId", required = false) Integer cropId,
            @RequestParam(value = "taskId", required = false) Integer taskId,
            @RequestParam(value = "vendorId", required = false) Integer vendorId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "paymentStatus", required = false) PaymentStatus paymentStatus,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "granularity", defaultValue = "MONTH") ExpenseTimeGranularity granularity) {

        ExpenseSearchCriteria criteria = ExpenseSearchCriteria.builder()
                .seasonId(seasonId)
                .plotId(plotId)
                .cropId(cropId)
                .taskId(taskId)
                .vendorId(vendorId)
                .category(category)
                .paymentStatus(paymentStatus)
                .fromDate(fromDate)
                .toDate(toDate)
                .q(q)
                .build();

        return ApiResponse.success(seasonExpenseService.analyticsTimeSeries(criteria, granularity));
    }

    @Operation(summary = "Export expenses to CSV (Admin)")
    @GetMapping("/expenses/export")
    public ResponseEntity<byte[]> exportExpenses(
            @RequestParam(value = "seasonId", required = false) Integer seasonId,
            @RequestParam(value = "plotId", required = false) Integer plotId,
            @RequestParam(value = "cropId", required = false) Integer cropId,
            @RequestParam(value = "taskId", required = false) Integer taskId,
            @RequestParam(value = "vendorId", required = false) Integer vendorId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "paymentStatus", required = false) PaymentStatus paymentStatus,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "q", required = false) String q) {

        ExpenseSearchCriteria criteria = ExpenseSearchCriteria.builder()
                .seasonId(seasonId)
                .plotId(plotId)
                .cropId(cropId)
                .taskId(taskId)
                .vendorId(vendorId)
                .category(category)
                .paymentStatus(paymentStatus)
                .fromDate(fromDate)
                .toDate(toDate)
                .q(q)
                .build();

        byte[] csvData = seasonExpenseService.exportCsv(criteria);
        String suffix = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String filename = "expenses-admin-" + suffix + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(csvData);
    }

    @Operation(summary = "Upload expense attachment (Admin)")
    @PostMapping(value = "/expenses/{id}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ExpenseResponse> uploadAttachment(
            @PathVariable Integer id,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(seasonExpenseService.uploadAttachment(id, file));
    }

    @Operation(summary = "Download expense attachment (Admin)")
    @GetMapping("/expenses/{id}/attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Integer id) {
        SeasonExpenseService.AttachmentDownload download = seasonExpenseService.downloadAttachment(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                download.getContentType() != null ? download.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE));
        headers.setContentLength(download.getSize());
        headers.setContentDisposition(ContentDisposition.attachment().filename(
                download.getFilename() != null ? download.getFilename() : "attachment").build());
        return ResponseEntity.ok().headers(headers).body(download.getResource());
    }
}
