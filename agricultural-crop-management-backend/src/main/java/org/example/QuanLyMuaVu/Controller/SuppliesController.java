package org.example.QuanLyMuaVu.Controller;

import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.DTO.Request.CreateSupplierRequest;
import org.example.QuanLyMuaVu.DTO.Request.StockInRequest;
import org.example.QuanLyMuaVu.DTO.Request.UpdateSupplierRequest;
import org.example.QuanLyMuaVu.DTO.Response.StockInResponse;
import org.example.QuanLyMuaVu.DTO.Response.SupplierResponse;
import org.example.QuanLyMuaVu.DTO.Response.SupplyItemResponse;
import org.example.QuanLyMuaVu.DTO.Response.SupplyLotResponse;
import org.example.QuanLyMuaVu.Service.SuppliesService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * REST endpoints for Suppliers & Supplies management (Farmer Portal).
 * Provides catalog views and Stock IN functionality.
 */
@RestController
@RequestMapping("/api/v1/supplies")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SuppliesController {

        SuppliesService suppliesService;

        // ===================================
        // CATALOG: SUPPLIERS
        // ===================================

        @Operation(summary = "List suppliers", description = "Get paginated list of suppliers with optional search")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        @PreAuthorize("hasRole('FARMER')")
        @GetMapping("/suppliers")
        public ApiResponse<PageResponse<SupplierResponse>> getSuppliers(
                        @RequestParam(value = "q", required = false) String q,
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "size", defaultValue = "20") int size) {
                Pageable pageable = PageRequest.of(page, size);
                return ApiResponse.success(suppliesService.getSuppliers(q, pageable));
        }

        @Operation(summary = "Get supplier by ID", description = "Get a single supplier by ID")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Supplier not found")
        })
        @PreAuthorize("hasRole('FARMER')")
        @GetMapping("/suppliers/{id}")
        public ApiResponse<SupplierResponse> getSupplier(@PathVariable Integer id) {
                return ApiResponse.success(suppliesService.getSupplierById(id));
        }

        @Operation(summary = "Create supplier", description = "Create a new supplier")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        @PreAuthorize("hasRole('FARMER')")
        @PostMapping("/suppliers")
        public ApiResponse<SupplierResponse> createSupplier(@Valid @RequestBody CreateSupplierRequest request) {
                return ApiResponse.success(suppliesService.createSupplier(request));
        }

        @Operation(summary = "Update supplier", description = "Update an existing supplier")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Supplier not found")
        })
        @PreAuthorize("hasRole('FARMER')")
        @PutMapping("/suppliers/{id}")
        public ApiResponse<SupplierResponse> updateSupplier(
                        @PathVariable Integer id,
                        @Valid @RequestBody UpdateSupplierRequest request) {
                return ApiResponse.success(suppliesService.updateSupplier(id, request));
        }

        @Operation(summary = "Delete supplier", description = "Delete a supplier if it has no associated supply lots")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Supplier has associated lots"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Supplier not found")
        })
        @PreAuthorize("hasRole('FARMER')")
        @DeleteMapping("/suppliers/{id}")
        public ApiResponse<Void> deleteSupplier(@PathVariable Integer id) {
                suppliesService.deleteSupplier(id);
                return ApiResponse.success(null);
        }

        // ===================================
        // CATALOG: SUPPLY ITEMS
        // ===================================

        @Operation(summary = "List supply items", description = "Get paginated list of supply items with optional filters")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        @PreAuthorize("hasRole('FARMER')")
        @GetMapping("/items")
        public ApiResponse<PageResponse<SupplyItemResponse>> getSupplyItems(
                        @RequestParam(value = "q", required = false) String q,
                        @RequestParam(value = "restricted", required = false) Boolean restricted,
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "size", defaultValue = "20") int size) {
                Pageable pageable = PageRequest.of(page, size);
                return ApiResponse.success(suppliesService.getSupplyItems(q, restricted, pageable));
        }

        // ===================================
        // CATALOG: SUPPLY LOTS
        // ===================================

        @Operation(summary = "List supply lots", description = "Get paginated list of supply lots with optional filters")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        @PreAuthorize("hasRole('FARMER')")
        @GetMapping("/lots")
        public ApiResponse<PageResponse<SupplyLotResponse>> getSupplyLots(
                        @RequestParam(value = "itemId", required = false) Integer itemId,
                        @RequestParam(value = "supplierId", required = false) Integer supplierId,
                        @RequestParam(value = "status", required = false) String status,
                        @RequestParam(value = "q", required = false) String q,
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "size", defaultValue = "20") int size) {
                Pageable pageable = PageRequest.of(page, size);
                return ApiResponse.success(suppliesService.getSupplyLots(itemId, supplierId, status, q, pageable));
        }

        // ===================================
        // STOCK IN
        // ===================================

        @Operation(summary = "Record Stock IN", description = "Create a new supply lot and record inbound stock movement")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request (validation failed or restricted confirmation required)"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden (warehouse access denied)"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Supplier, item, warehouse, or location not found")
        })
        @PreAuthorize("hasRole('FARMER')")
        @PostMapping("/stock-in")
        public ApiResponse<StockInResponse> stockIn(@Valid @RequestBody StockInRequest request) {
                return ApiResponse.success(suppliesService.stockIn(request));
        }
}
