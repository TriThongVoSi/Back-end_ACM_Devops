package org.example.QuanLyMuaVu.Controller.Admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.DTO.Response.InventoryLotDetailResponse;
import org.example.QuanLyMuaVu.DTO.Response.InventoryLotMovementResponse;
import org.example.QuanLyMuaVu.DTO.Response.InventoryOptionsResponse;
import org.example.QuanLyMuaVu.DTO.Response.InventoryRiskLotResponse;
import org.example.QuanLyMuaVu.Service.Admin.AdminInventoryHealthService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin Inventory Controller for risk lot drill-down.
 */
@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminInventoryController {

    private final AdminInventoryHealthService adminInventoryHealthService;

    @GetMapping("/lots")
    public ApiResponse<PageResponse<InventoryRiskLotResponse>> getRiskLots(
            @RequestParam(value = "farmId", required = false) Integer farmId,
            @RequestParam(value = "status", defaultValue = "ALL") String status,
            @RequestParam(value = "windowDays", defaultValue = "30") Integer windowDays,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "lowStockThreshold", required = false) java.math.BigDecimal lowStockThreshold,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        log.info("Admin requesting risk lots farmId={}, status={}, windowDays={}", farmId, status, windowDays);
        return ApiResponse.success(
                adminInventoryHealthService.getRiskLots(farmId, status, windowDays, q, sort, lowStockThreshold, page,
                        limit));
    }

    @GetMapping("/lots/{lotId}")
    public ApiResponse<InventoryLotDetailResponse> getLotDetail(@PathVariable Integer lotId) {
        return ApiResponse.success(adminInventoryHealthService.getLotDetail(lotId));
    }

    @GetMapping("/lots/{lotId}/movements")
    public ApiResponse<java.util.List<InventoryLotMovementResponse>> getLotMovements(@PathVariable Integer lotId) {
        return ApiResponse.success(adminInventoryHealthService.getLotMovements(lotId));
    }

    @GetMapping("/options")
    public ApiResponse<InventoryOptionsResponse> getInventoryOptions() {
        return ApiResponse.success(adminInventoryHealthService.getInventoryOptions());
    }
}
