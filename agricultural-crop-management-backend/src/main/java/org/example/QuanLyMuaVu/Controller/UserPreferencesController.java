package org.example.QuanLyMuaVu.Controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Request.UserPreferencesUpdateRequest;
import org.example.QuanLyMuaVu.DTO.Response.UserPreferencesResponse;
import org.example.QuanLyMuaVu.Service.UserPreferencesService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserPreferencesController {

    UserPreferencesService userPreferencesService;

    @PreAuthorize("hasAnyRole('ADMIN','FARMER','BUYER')")
    @GetMapping("/me")
    public ApiResponse<UserPreferencesResponse> getMyPreferences() {
        return ApiResponse.success(userPreferencesService.getMyPreferences());
    }

    @PreAuthorize("hasAnyRole('ADMIN','FARMER','BUYER')")
    @PutMapping("/me")
    public ApiResponse<UserPreferencesResponse> updateMyPreferences(
            @RequestBody UserPreferencesUpdateRequest request) {
        return ApiResponse.success(userPreferencesService.updateMyPreferences(request));
    }
}
