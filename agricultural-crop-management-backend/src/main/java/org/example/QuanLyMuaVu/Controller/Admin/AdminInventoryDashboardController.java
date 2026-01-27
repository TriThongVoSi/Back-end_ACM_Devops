package org.example.QuanLyMuaVu.Controller.Admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Response.InventoryHealthResponse;
import org.example.QuanLyMuaVu.Service.Admin.AdminInventoryHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin Inventory Health Dashboard Controller
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminInventoryDashboardController {

    private final AdminInventoryHealthService adminInventoryHealthService;

    @GetMapping("/inventory-health")
    public ResponseEntity<ApiResponse<InventoryHealthResponse>> getInventoryHealth(
            @RequestParam(value = "windowDays", defaultValue = "30") Integer windowDays,
            @RequestParam(value = "includeExpiring", defaultValue = "true") Boolean includeExpiring,
            @RequestParam(value = "limit", defaultValue = "5") Integer limit) {
        log.info("Admin requesting inventory health widget data");
        InventoryHealthResponse response = adminInventoryHealthService.getInventoryHealth(windowDays, includeExpiring,
                limit);
        return ResponseEntity.ok(ApiResponse.success("Inventory health retrieved", response));
    }
}
