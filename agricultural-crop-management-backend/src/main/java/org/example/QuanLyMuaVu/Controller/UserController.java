package org.example.QuanLyMuaVu.Controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Request.FarmerUpdateRequest;
import org.example.QuanLyMuaVu.DTO.Request.UserProfileUpdateRequest;
import org.example.QuanLyMuaVu.DTO.Response.FarmerResponse;
import org.example.QuanLyMuaVu.Service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;

    @PreAuthorize("hasAnyRole('BUYER','FARMER')")
    @GetMapping("/me")
    public ApiResponse<FarmerResponse> me() {
        return ApiResponse.success(userService.getMyInfo());
    }

    @PreAuthorize("hasAnyRole('BUYER','FARMER')")
    @PutMapping("/profile")
    public ApiResponse<FarmerResponse> updateProfile(@RequestBody UserProfileUpdateRequest request) {
        return ApiResponse.success(userService.updateProfile(request));
    }

    @PreAuthorize("hasAnyRole('BUYER','FARMER')")
    @PutMapping("/change-password")
    public ApiResponse<FarmerResponse> changePassword(@Valid @RequestBody FarmerUpdateRequest request) {
        return ApiResponse.success(userService.changeMyPassword(request));
    }
}
