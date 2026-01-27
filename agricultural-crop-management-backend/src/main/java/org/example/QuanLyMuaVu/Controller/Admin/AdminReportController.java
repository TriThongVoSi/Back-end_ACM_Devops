package org.example.QuanLyMuaVu.Controller.Admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Request.AdminReportFilter;
import org.example.QuanLyMuaVu.DTO.Response.AdminReportAnalyticsResponse;
import org.example.QuanLyMuaVu.DTO.Response.AdminReportOptionsResponse;
import org.example.QuanLyMuaVu.DTO.Response.AdminReportResponse;
import org.example.QuanLyMuaVu.Enums.AdminReportTab;
import org.example.QuanLyMuaVu.Enums.ExpenseTimeGranularity;
import org.example.QuanLyMuaVu.Service.Admin.AdminReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Admin Report Controller
 * Provides financial and operational reports for admin.
 */
@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

        private final AdminReportService adminReportService;

        /**
         * Build AdminReportFilter from request parameters.
         */
        private AdminReportFilter buildFilter(
                        Integer year,
                        LocalDate dateFrom,
                        LocalDate dateTo,
                        Integer cropId,
                        Integer farmId,
                        Integer plotId,
                        Integer varietyId) {
                return AdminReportFilter.builder()
                                .year(year)
                                .fromDate(dateFrom)
                                .toDate(dateTo)
                                .cropId(cropId)
                                .farmId(farmId)
                                .plotId(plotId)
                                .varietyId(varietyId)
                                .build();
        }

        /**
         * GET /api/v1/admin/reports/yield
         * Returns yield/harvest report with variance calculations
         */
        @GetMapping("/yield")
        public ResponseEntity<ApiResponse<?>> getYieldReport(
                        @RequestParam(required = false) Integer year,
                        @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                        @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                        @RequestParam(required = false) Integer cropId,
                        @RequestParam(required = false) Integer farmId,
                        @RequestParam(required = false) Integer plotId,
                        @RequestParam(required = false) Integer varietyId,
                        @RequestParam(defaultValue = "false") boolean analytics) {
                log.info("Admin requesting yield report with filters: year={}, cropId={}, farmId={}, plotId={}, varietyId={}, analytics={}",
                                year, cropId, farmId, plotId, varietyId, analytics);

                AdminReportFilter filter = buildFilter(year, dateFrom, dateTo, cropId, farmId, plotId, varietyId);
                if (analytics) {
                        AdminReportAnalyticsResponse.YieldTabResponse report = adminReportService.getYieldAnalytics(filter);
                        return ResponseEntity.ok(ApiResponse.success("Yield report generated", report));
                }
                List<AdminReportResponse.YieldReport> report = adminReportService.getYieldReport(filter);
                return ResponseEntity.ok(ApiResponse.success("Yield report generated", report));
        }

        /**
         * GET /api/v1/admin/reports/cost
         * Returns cost/expense report with cost per kg calculations
         */
        @GetMapping("/cost")
        public ResponseEntity<ApiResponse<?>> getCostReport(
                        @RequestParam(required = false) Integer year,
                        @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                        @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                        @RequestParam(required = false) Integer cropId,
                        @RequestParam(required = false) Integer farmId,
                        @RequestParam(required = false) Integer plotId,
                        @RequestParam(required = false) Integer varietyId,
                        @RequestParam(required = false) ExpenseTimeGranularity granularity,
                        @RequestParam(defaultValue = "false") boolean analytics) {
                log.info("Admin requesting cost report with filters: year={}, cropId={}, farmId={}, plotId={}, varietyId={}, analytics={}",
                                year, cropId, farmId, plotId, varietyId, analytics);

                AdminReportFilter filter = buildFilter(year, dateFrom, dateTo, cropId, farmId, plotId, varietyId);
                if (analytics) {
                        AdminReportAnalyticsResponse.CostTabResponse report = adminReportService.getCostAnalytics(filter,
                                        granularity);
                        return ResponseEntity.ok(ApiResponse.success("Cost report generated", report));
                }
                List<AdminReportResponse.CostReport> report = adminReportService.getCostReport(filter);
                return ResponseEntity.ok(ApiResponse.success("Cost report generated", report));
        }

        /**
         * GET /api/v1/admin/reports/revenue
         * Returns revenue report with average price calculations
         */
        @GetMapping("/revenue")
        public ResponseEntity<ApiResponse<?>> getRevenueReport(
                        @RequestParam(required = false) Integer year,
                        @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                        @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                        @RequestParam(required = false) Integer cropId,
                        @RequestParam(required = false) Integer farmId,
                        @RequestParam(required = false) Integer plotId,
                        @RequestParam(required = false) Integer varietyId,
                        @RequestParam(defaultValue = "false") boolean analytics) {
                log.info("Admin requesting revenue report with filters: year={}, cropId={}, farmId={}, plotId={}, varietyId={}, analytics={}",
                                year, cropId, farmId, plotId, varietyId, analytics);

                AdminReportFilter filter = buildFilter(year, dateFrom, dateTo, cropId, farmId, plotId, varietyId);
                if (analytics) {
                        AdminReportAnalyticsResponse.RevenueTabResponse report = adminReportService
                                        .getRevenueAnalytics(filter);
                        return ResponseEntity.ok(ApiResponse.success("Revenue report generated", report));
                }
                List<AdminReportResponse.RevenueReport> report = adminReportService.getRevenueReport(filter);
                return ResponseEntity.ok(ApiResponse.success("Revenue report generated", report));
        }

        /**
         * GET /api/v1/admin/reports/profit
         * Returns profit report with margin calculations
         */
        @GetMapping("/profit")
        public ResponseEntity<ApiResponse<?>> getProfitReport(
                        @RequestParam(required = false) Integer year,
                        @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                        @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                        @RequestParam(required = false) Integer cropId,
                        @RequestParam(required = false) Integer farmId,
                        @RequestParam(required = false) Integer plotId,
                        @RequestParam(required = false) Integer varietyId,
                        @RequestParam(defaultValue = "false") boolean analytics) {
                log.info("Admin requesting profit report with filters: year={}, cropId={}, farmId={}, plotId={}, varietyId={}, analytics={}",
                                year, cropId, farmId, plotId, varietyId, analytics);

                AdminReportFilter filter = buildFilter(year, dateFrom, dateTo, cropId, farmId, plotId, varietyId);
                if (analytics) {
                        AdminReportAnalyticsResponse.ProfitTabResponse report = adminReportService.getProfitAnalytics(filter);
                        return ResponseEntity.ok(ApiResponse.success("Profit report generated", report));
                }
                List<AdminReportResponse.ProfitReport> report = adminReportService.getProfitReport(filter);
                return ResponseEntity.ok(ApiResponse.success("Profit report generated", report));
        }

        /**
         * GET /api/v1/admin/reports/summary
         * Returns summary report for date range
         */
        @GetMapping("/summary")
        public ResponseEntity<ApiResponse<AdminReportAnalyticsResponse.Summary>> getSummary(
                        @RequestParam(required = false) Integer year,
                        @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                        @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                        @RequestParam(required = false) Integer cropId,
                        @RequestParam(required = false) Integer farmId,
                        @RequestParam(required = false) Integer plotId,
                        @RequestParam(required = false) Integer varietyId) {
                log.info("Admin requesting summary report");

                AdminReportFilter filter = buildFilter(year, dateFrom, dateTo, cropId, farmId, plotId, varietyId);
                AdminReportAnalyticsResponse.Summary summary = adminReportService.getSummary(filter);

                return ResponseEntity.ok(ApiResponse.success("Summary report generated", summary));
        }

        /**
         * GET /api/v1/admin/reports/export
         * Export report data to CSV.
         */
        @GetMapping("/export")
        public ResponseEntity<byte[]> exportReport(
                        @RequestParam AdminReportTab tab,
                        @RequestParam(required = false) Integer year,
                        @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                        @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                        @RequestParam(required = false) Integer cropId,
                        @RequestParam(required = false) Integer farmId,
                        @RequestParam(required = false) Integer plotId,
                        @RequestParam(required = false) Integer varietyId,
                        @RequestParam(required = false) ExpenseTimeGranularity granularity) {
                log.info("Admin exporting report: tab={}, filters: year={}, cropId={}, farmId={}, plotId={}, varietyId={}",
                                tab, year, cropId, farmId, plotId, varietyId);

                AdminReportFilter filter = buildFilter(year, dateFrom, dateTo, cropId, farmId, plotId, varietyId);
                AdminReportService.ReportExport export = adminReportService.exportReport(filter, tab, granularity);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("text/csv"));
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.filename() + "\"");
                return ResponseEntity.ok()
                                .headers(headers)
                                .body(export.data());
        }

        /**
         * GET /api/v1/admin/reports/options
         * Returns filter options for admin report selects.
         */
        @GetMapping("/options")
        public ResponseEntity<ApiResponse<AdminReportOptionsResponse>> getOptions() {
                AdminReportOptionsResponse options = adminReportService.getReportOptions();
                return ResponseEntity.ok(ApiResponse.success("Report options retrieved", options));
        }
}
