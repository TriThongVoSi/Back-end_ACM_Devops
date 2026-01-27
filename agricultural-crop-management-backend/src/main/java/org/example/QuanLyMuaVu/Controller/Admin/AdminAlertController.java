package org.example.QuanLyMuaVu.Controller.Admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.DTO.Request.AlertRefreshRequest;
import org.example.QuanLyMuaVu.DTO.Request.AlertSendRequest;
import org.example.QuanLyMuaVu.DTO.Request.AlertStatusUpdateRequest;
import org.example.QuanLyMuaVu.DTO.Response.AdminAlertResponse;
import org.example.QuanLyMuaVu.Service.Admin.AdminAlertService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/alerts")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminAlertController {

    private final AdminAlertService adminAlertService;

    @GetMapping
    public ApiResponse<PageResponse<AdminAlertResponse>> listAlerts(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "severity", required = false) String severity,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "farmId", required = false) Integer farmId,
            @RequestParam(value = "windowDays", required = false) Integer windowDays,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ApiResponse.success(adminAlertService.listAlerts(type, severity, status, farmId, windowDays, page, limit));
    }

    @PostMapping("/refresh")
    public ApiResponse<List<AdminAlertResponse>> refreshAlerts(@RequestBody(required = false) AlertRefreshRequest request) {
        Integer windowDays = request != null ? request.getWindowDays() : null;
        return ApiResponse.success(adminAlertService.refreshAlerts(windowDays));
    }

    @PostMapping("/{id}/send")
    public ApiResponse<AdminAlertResponse> sendAlert(@PathVariable("id") Long alertId,
            @RequestBody AlertSendRequest request) {
        return ApiResponse.success(adminAlertService.sendAlert(alertId, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AdminAlertResponse> updateStatus(@PathVariable("id") Long alertId,
            @RequestBody AlertStatusUpdateRequest request) {
        return ApiResponse.success(adminAlertService.updateStatus(alertId, request.getStatus()));
    }
}
