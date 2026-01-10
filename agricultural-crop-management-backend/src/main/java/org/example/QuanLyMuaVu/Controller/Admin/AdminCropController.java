package org.example.QuanLyMuaVu.Controller.Admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Request.CropRequest;
import org.example.QuanLyMuaVu.DTO.Response.CropResponse;
import org.example.QuanLyMuaVu.Service.CropService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin REST endpoints for system-wide crop management.
 * Provides CRUD operations for crop definitions that are used across all farms.
 */
@RestController
@RequestMapping("/api/v1/admin/crops")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Crops", description = "Admin endpoints for system-wide crop management")
public class AdminCropController {

    CropService cropService;

    @Operation(summary = "List all crops (Admin)", description = "Get list of all crops in the system")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public ApiResponse<List<CropResponse>> listCrops() {
        return ApiResponse.success(cropService.getAll());
    }

    @Operation(summary = "Create crop (Admin)", description = "Create a new crop definition")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping
    public ApiResponse<CropResponse> createCrop(@Valid @RequestBody CropRequest request) {
        return ApiResponse.success(cropService.create(request));
    }

    @Operation(summary = "Update crop (Admin)", description = "Update an existing crop definition")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found")
    })
    @PutMapping("/{id}")
    public ApiResponse<CropResponse> updateCrop(
            @PathVariable Integer id,
            @Valid @RequestBody CropRequest request) {
        return ApiResponse.success(cropService.update(id, request));
    }

    @Operation(summary = "Delete crop", description = "Delete an existing crop. Fails if crop has varieties or is referenced in seasons.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Crop has varieties or is referenced in seasons")
    })
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCrop(@PathVariable Integer id) {
        cropService.delete(id);
        return ApiResponse.success(null);
    }
}
