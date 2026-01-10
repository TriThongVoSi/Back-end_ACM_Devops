package org.example.QuanLyMuaVu.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.DTO.Request.StockInRequest;
import org.example.QuanLyMuaVu.DTO.Response.StockInResponse;
import org.example.QuanLyMuaVu.DTO.Response.StockMovementResponse;
import org.example.QuanLyMuaVu.DTO.Response.SupplierResponse;
import org.example.QuanLyMuaVu.DTO.Response.SupplyItemResponse;
import org.example.QuanLyMuaVu.DTO.Response.SupplyLotResponse;
import org.example.QuanLyMuaVu.Entity.StockLocation;
import org.example.QuanLyMuaVu.Entity.StockMovement;
import org.example.QuanLyMuaVu.Entity.Supplier;
import org.example.QuanLyMuaVu.Entity.SupplyItem;
import org.example.QuanLyMuaVu.Entity.SupplyLot;
import org.example.QuanLyMuaVu.Entity.Warehouse;
import org.example.QuanLyMuaVu.Enums.StockMovementType;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.StockLocationRepository;
import org.example.QuanLyMuaVu.Repository.StockMovementRepository;
import org.example.QuanLyMuaVu.Repository.SupplierRepository;
import org.example.QuanLyMuaVu.Repository.SupplyItemRepository;
import org.example.QuanLyMuaVu.Repository.SupplyLotRepository;
import org.example.QuanLyMuaVu.Repository.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class SuppliesService {

    SupplierRepository supplierRepository;
    SupplyItemRepository supplyItemRepository;
    SupplyLotRepository supplyLotRepository;
    WarehouseRepository warehouseRepository;
    StockLocationRepository stockLocationRepository;
    StockMovementRepository stockMovementRepository;
    FarmAccessService farmAccessService;

    // ============================================
    // CATALOG: SUPPLIERS
    // ============================================
    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> getSuppliers(String q, Pageable pageable) {
        String searchQuery = (q != null && !q.isBlank()) ? q.trim() : null;
        Page<Supplier> page = supplierRepository.searchByName(searchQuery, pageable);
        List<SupplierResponse> items = page.getContent().stream()
                .map(this::toSupplierResponse)
                .collect(Collectors.toList());
        return PageResponse.of(page, items);
    }

    // ============================================
    // CATALOG: SUPPLY ITEMS
    // ============================================
    @Transactional(readOnly = true)
    public PageResponse<SupplyItemResponse> getSupplyItems(String q, Boolean restricted, Pageable pageable) {
        String searchQuery = (q != null && !q.isBlank()) ? q.trim() : null;
        Page<SupplyItem> page = supplyItemRepository.searchItems(searchQuery, restricted, pageable);
        List<SupplyItemResponse> items = page.getContent().stream()
                .map(this::toSupplyItemResponse)
                .collect(Collectors.toList());
        return PageResponse.of(page, items);
    }

    // ============================================
    // CATALOG: SUPPLY LOTS
    // ============================================
    @Transactional(readOnly = true)
    public PageResponse<SupplyLotResponse> getSupplyLots(Integer itemId, Integer supplierId,
            String status, String q, Pageable pageable) {
        String searchQuery = (q != null && !q.isBlank()) ? q.trim() : null;
        String statusFilter = (status != null && !status.isBlank()) ? status.trim() : null;
        Page<SupplyLot> page = supplyLotRepository.searchLots(itemId, supplierId, statusFilter, searchQuery, pageable);
        List<SupplyLotResponse> items = page.getContent().stream()
                .map(this::toSupplyLotResponse)
                .collect(Collectors.toList());
        return PageResponse.of(page, items);
    }

    // ============================================
    // STOCK IN
    // ============================================
    public StockInResponse stockIn(StockInRequest request) {
        // 1. Validate warehouse ownership
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        ensureWarehouseOwnership(warehouse);

        // 2. Validate location if provided
        StockLocation location = null;
        if (request.getLocationId() != null) {
            location = stockLocationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
            if (!location.getWarehouse().getId().equals(warehouse.getId())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }

        // 3. Load supplier
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_FOUND));

        // 4. Load supply item and check restricted flag
        SupplyItem supplyItem = supplyItemRepository.findById(request.getSupplyItemId())
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLY_ITEM_NOT_FOUND));

        // 5. If restricted item, require confirmation
        if (Boolean.TRUE.equals(supplyItem.getRestrictedFlag())) {
            if (!Boolean.TRUE.equals(request.getConfirmRestricted())) {
                throw new AppException(ErrorCode.RESTRICTED_CONFIRM_REQUIRED);
            }
        }

        // 6. Parse expiry date if provided
        LocalDate expiryDate = null;
        if (request.getExpiryDate() != null && !request.getExpiryDate().isBlank()) {
            expiryDate = LocalDate.parse(request.getExpiryDate());
        }

        // 7. Create SupplyLot
        SupplyLot lot = SupplyLot.builder()
                .supplyItem(supplyItem)
                .supplier(supplier)
                .batchCode(request.getBatchCode())
                .expiryDate(expiryDate)
                .status("IN_STOCK")
                .build();
        lot = supplyLotRepository.save(lot);

        // 8. Create StockMovement (type IN, positive quantity)
        StockMovement movement = StockMovement.builder()
                .supplyLot(lot)
                .warehouse(warehouse)
                .location(location)
                .movementType(StockMovementType.IN)
                .quantity(request.getQuantity().abs()) // Always positive for IN
                .movementDate(LocalDateTime.now())
                .note(request.getNote() != null ? request.getNote() : "Stock IN via Suppliers & Supplies")
                .build();
        movement = stockMovementRepository.save(movement);

        // 9. Return response
        return StockInResponse.builder()
                .supplyLot(toSupplyLotResponse(lot))
                .movement(toStockMovementResponse(movement))
                .build();
    }

    // ============================================
    // HELPER METHODS
    // ============================================
    private void ensureWarehouseOwnership(Warehouse warehouse) {
        if (warehouse.getFarm() == null) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        farmAccessService.assertCurrentUserCanAccessFarm(warehouse.getFarm());
    }

    private SupplierResponse toSupplierResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .licenseNo(supplier.getLicenseNo())
                .contactEmail(supplier.getContactEmail())
                .contactPhone(supplier.getContactPhone())
                .build();
    }

    private SupplyItemResponse toSupplyItemResponse(SupplyItem item) {
        return SupplyItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .activeIngredient(item.getActiveIngredient())
                .unit(item.getUnit())
                .restrictedFlag(Boolean.TRUE.equals(item.getRestrictedFlag()))
                .build();
    }

    private SupplyLotResponse toSupplyLotResponse(SupplyLot lot) {
        return SupplyLotResponse.builder()
                .id(lot.getId())
                .batchCode(lot.getBatchCode())
                .expiryDate(lot.getExpiryDate())
                .status(lot.getStatus())
                .supplierId(lot.getSupplier() != null ? lot.getSupplier().getId() : null)
                .supplierName(lot.getSupplier() != null ? lot.getSupplier().getName() : null)
                .supplyItemId(lot.getSupplyItem() != null ? lot.getSupplyItem().getId() : null)
                .supplyItemName(lot.getSupplyItem() != null ? lot.getSupplyItem().getName() : null)
                .unit(lot.getSupplyItem() != null ? lot.getSupplyItem().getUnit() : null)
                .restrictedFlag(
                        lot.getSupplyItem() != null && Boolean.TRUE.equals(lot.getSupplyItem().getRestrictedFlag()))
                .build();
    }

    private StockMovementResponse toStockMovementResponse(StockMovement movement) {
        return StockMovementResponse.builder()
                .id(movement.getId())
                .supplyLotId(movement.getSupplyLot() != null ? movement.getSupplyLot().getId() : null)
                .batchCode(movement.getSupplyLot() != null ? movement.getSupplyLot().getBatchCode() : null)
                .supplyItemName(movement.getSupplyLot() != null && movement.getSupplyLot().getSupplyItem() != null
                        ? movement.getSupplyLot().getSupplyItem().getName()
                        : null)
                .unit(movement.getSupplyLot() != null && movement.getSupplyLot().getSupplyItem() != null
                        ? movement.getSupplyLot().getSupplyItem().getUnit()
                        : null)
                .warehouseId(movement.getWarehouse() != null ? movement.getWarehouse().getId() : null)
                .warehouseName(movement.getWarehouse() != null ? movement.getWarehouse().getName() : null)
                .locationId(movement.getLocation() != null ? movement.getLocation().getId() : null)
                .locationLabel(movement.getLocation() != null ? buildLocationLabel(movement.getLocation()) : null)
                .movementType(movement.getMovementType() != null ? movement.getMovementType().name() : null)
                .quantity(movement.getQuantity())
                .movementDate(movement.getMovementDate())
                .seasonId(movement.getSeason() != null ? movement.getSeason().getId() : null)
                .seasonName(movement.getSeason() != null ? movement.getSeason().getSeasonName() : null)
                .taskId(movement.getTask() != null ? movement.getTask().getId() : null)
                .taskTitle(movement.getTask() != null ? movement.getTask().getTitle() : null)
                .note(movement.getNote())
                .build();
    }

    private String buildLocationLabel(StockLocation location) {
        StringBuilder sb = new StringBuilder();
        if (location.getZone() != null)
            sb.append(location.getZone());
        if (location.getAisle() != null) {
            if (sb.length() > 0)
                sb.append("-");
            sb.append(location.getAisle());
        }
        if (location.getShelf() != null) {
            if (sb.length() > 0)
                sb.append("-");
            sb.append(location.getShelf());
        }
        if (location.getBin() != null) {
            if (sb.length() > 0)
                sb.append("-");
            sb.append(location.getBin());
        }
        return sb.length() > 0 ? sb.toString() : "Location " + location.getId();
    }
}
