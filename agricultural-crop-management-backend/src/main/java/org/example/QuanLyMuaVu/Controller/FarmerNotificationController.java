package org.example.QuanLyMuaVu.Controller;

import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Response.FarmerNotificationResponse;
import org.example.QuanLyMuaVu.Service.FarmerNotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/farmer/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FARMER')")
public class FarmerNotificationController {

    private final FarmerNotificationService farmerNotificationService;

    @GetMapping
    public ApiResponse<List<FarmerNotificationResponse>> listNotifications() {
        return ApiResponse.success(farmerNotificationService.listMyNotifications());
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<FarmerNotificationResponse> markAsRead(@PathVariable("id") Long notificationId) {
        return ApiResponse.success(farmerNotificationService.markAsRead(notificationId));
    }
}
