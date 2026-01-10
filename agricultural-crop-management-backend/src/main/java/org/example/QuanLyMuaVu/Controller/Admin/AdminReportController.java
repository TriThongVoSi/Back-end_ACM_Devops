package org.example.QuanLyMuaVu.Controller.Admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Request.AdminReportFilter;
import org.example.QuanLyMuaVu.DTO.Response.AdminReportResponse;
import org.example.QuanLyMuaVu.Service.Admin.AdminReportService;
import org.springframework.format.annotation.DateTimeFormat;
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
                        LocalDate fromDate,
                        LocalDate toDate,
                        Integer cropId,
                        Integer farmId,
                        Integer plotId) {
                return AdminReportFilter.builder()
                                .year(year)
                                .fromDate(fromDate)
                                .toDate(toDate)
                                .cropId(cropId)
                                .farmId(farmId)
                                .plotId(plotId)
                                .build();
        }

        /**
         * GET /api/v1/admin/reports/yield
         * Returns yield/harvest report with variance calculations
         */
        @GetMapping("/yield")
        public ResponseEntity<ApiResponse<List<AdminReportResponse.YieldReport>>> getYieldReport(
                        @RequestParam(required = false) Integer year,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                        @RequestParam(required = false) Integer cropId,
                        @RequestParam(required = false) Integer farmId,
                        @RequestParam(required = false) Integer plotId) {
                log.info("Admin requesting yield report with filters: year={}, cropId={}, farmId={}, plotId={}",
                                year, cropId, farmId, plotId);

                AdminReportFilter filter = buildFilter(year, fromDate, toDate, cropId, farmId, plotId);
                List<AdminReportResponse.YieldReport> report = adminReportService.getYieldReport(filter);

                return ResponseEntity.ok(ApiResponse.success("Yield report generated", report));
        }

        /**
         * GET /api/v1/admin/reports/cost
         * Returns cost/expense report with cost per kg calculations
         */
        @GetMapping("/cost")
        public ResponseEntity<ApiResponse<List<AdminReportResponse.CostReport>>> getCostReport(
                        @RequestParam(required = false) Integer year,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                        @RequestParam(required = false) Integer cropId,
                        @RequestParam(required = false) Integer farmId,
                        @RequestParam(required = false) Integer plotId) {
                log.info("Admin requesting cost report with filters: year={}, cropId={}, farmId={}, plotId={}",
                                year, cropId, farmId, plotId);

                AdminReportFilter filter = buildFilter(year, fromDate, toDate, cropId, farmId, plotId);
                List<AdminReportResponse.CostReport> report = adminReportService.getCostReport(filter);

                return ResponseEntity.ok(ApiResponse.success("Cost report generated", report));
        }

        /**
         * GET /api/v1/admin/reports/revenue
         * Returns revenue report with average price calculations
         */
        @GetMapping("/revenue")
        public ResponseEntity<ApiResponse<List<AdminReportResponse.RevenueReport>>> getRevenueReport(
                        @RequestParam(required = false) Integer year,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                        @RequestParam(required = false) Integer cropId,
                        @RequestParam(required = false) Integer farmId,
                        @RequestParam(required = false) Integer plotId) {
                log.info("Admin requesting revenue report with filters: year={}, cropId={}, farmId={}, plotId={}",
                                year, cropId, farmId, plotId);

                AdminReportFilter filter = buildFilter(year, fromDate, toDate, cropId, farmId, plotId);
                List<AdminReportResponse.RevenueReport> report = adminReportService.getRevenueReport(filter);

                return ResponseEntity.ok(ApiResponse.success("Revenue report generated", report));
        }

        /**
         * GET /api/v1/admin/reports/profit
         * Returns profit report with margin calculations
         */
        @GetMapping("/profit")
        public ResponseEntity<ApiResponse<List<AdminReportResponse.ProfitReport>>> getProfitReport(
                        @RequestParam(required = false) Integer year,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                        @RequestParam(required = false) Integer cropId,
                        @RequestParam(required = false) Integer farmId,
                        @RequestParam(required = false) Integer plotId) {
                log.info("Admin requesting profit report with filters: year={}, cropId={}, farmId={}, plotId={}",
                                year, cropId, farmId, plotId);

                AdminReportFilter filter = buildFilter(year, fromDate, toDate, cropId, farmId, plotId);
                List<AdminReportResponse.ProfitReport> report = adminReportService.getProfitReport(filter);

                return ResponseEntity.ok(ApiResponse.success("Profit report generated", report));
        }

        /**
         * GET /api/v1/admin/reports/summary
         * Returns summary report for date range
         */
        @GetMapping("/summary")
        public ResponseEntity<ApiResponse<AdminReportService.ReportSummary>> getSummary(
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                log.info("Admin requesting summary report");

                AdminReportService.ReportSummary summary = adminReportService.getSummary(startDate, endDate);

                return ResponseEntity.ok(ApiResponse.success("Summary report generated", summary));
        }
}
