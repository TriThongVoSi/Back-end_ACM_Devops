package org.example.QuanLyMuaVu.Controller.Admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Response.DashboardStatsDTO;
import org.example.QuanLyMuaVu.Service.Admin.AdminDashboardFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin Dashboard Controller
 * Provides system-wide statistics for admin dashboard.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardFacade adminDashboardFacade;

    /**
     * GET /api/v1/admin/dashboard-stats
     * Returns system-wide dashboard statistics
     */
    @GetMapping("/dashboard-stats")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getDashboardStats() {
        log.info("Admin requesting dashboard stats");

        DashboardStatsDTO stats = adminDashboardFacade.getDashboardStats();

        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved", stats));
    }
}
