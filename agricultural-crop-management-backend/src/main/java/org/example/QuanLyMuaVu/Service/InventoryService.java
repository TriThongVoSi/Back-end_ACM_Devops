package org.example.QuanLyMuaVu.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.DTO.Request.RecordStockMovementRequest;
import org.example.QuanLyMuaVu.DTO.Response.OnHandRowResponse;
import org.example.QuanLyMuaVu.DTO.Response.StockLocationResponse;
import org.example.QuanLyMuaVu.DTO.Response.StockMovementResponse;
import org.example.QuanLyMuaVu.DTO.Response.WarehouseResponse;
import org.example.QuanLyMuaVu.Entity.Farm;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Entity.StockLocation;
import org.example.QuanLyMuaVu.Entity.StockMovement;
import org.example.QuanLyMuaVu.Entity.SupplyLot;
import org.example.QuanLyMuaVu.Entity.Warehouse;
import org.example.QuanLyMuaVu.Enums.StockMovementType;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.SeasonRepository;
import org.example.QuanLyMuaVu.Repository.StockLocationRepository;
import org.example.QuanLyMuaVu.Repository.StockMovementRepository;
import org.example.QuanLyMuaVu.Repository.SupplyLotRepository;
import org.example.QuanLyMuaVu.Repository.TaskRepository;
import org.example.QuanLyMuaVu.Repository.WarehouseRepository;
import org.example.QuanLyMuaVu.Repository.FarmRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class InventoryService {

    WarehouseRepository warehouseRepository;
    StockLocationRepository stockLocationRepository;
    SupplyLotRepository supplyLotRepository;
    StockMovementRepository stockMovementRepository;
    FarmRepository farmRepository;
    SeasonRepository seasonRepository;
    TaskRepository taskRepository;
    FarmAccessService farmAccessService;

    // ============================================
    // GET MY WAREHOUSES
    // ============================================
    public List<WarehouseResponse> getMyWarehouses() {
        List<Integer> farmIds = farmAccessService.getAccessibleFarmIdsForCurrentUser();
        if (farmIds.isEmpty()) {
            return List.of();
        }
        List<Farm> myFarms = farmRepository.findAllById(farmIds);
        List<Warehouse> warehouses = warehouseRepository.findByFarmIn(myFarms);
        return warehouses.stream()
                .map(this::toWarehouseResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // GET LOCATIONS BY WAREHOUSE
    // ============================================
    public List<StockLocationResponse> getLocationsByWarehouse(Integer warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        ensureWarehouseOwnership(warehouse);

        List<StockLocation> locations = stockLocationRepository.findAllByWarehouse(warehouse);
        return locations.stream()
                .map(this::toStockLocationResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // GET ON-HAND LIST (Paginated)
    // ============================================
    public PageResponse<OnHandRowResponse> getOnHandList(Integer warehouseId, Integer locationId,
            Integer lotId, String q, Pageable pageable) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        ensureWarehouseOwnership(warehouse);

        StockLocation location = null;
        if (locationId != null) {
            location = stockLocationRepository.findById(locationId)
                    .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
            if (!location.getWarehouse().getId().equals(warehouse.getId())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }

        // Get all distinct supply lot IDs with movements at this warehouse/location
        List<Integer> lotIds = stockMovementRepository.findDistinctSupplyLotIdsByWarehouse(warehouse, location);

        if (lotIds.isEmpty()) {
            return createEmptyPageResponse(pageable);
        }

        // Filter by specific lot if provided
        if (lotId != null) {
            if (!lotIds.contains(lotId)) {
                return createEmptyPageResponse(pageable);
            }
            lotIds = List.of(lotId);
        }

        // Build on-hand rows
        List<OnHandRowResponse> allRows = new ArrayList<>();
        for (Integer lid : lotIds) {
            SupplyLot lot = supplyLotRepository.findById(lid).orElse(null);
            if (lot == null)
                continue;

            // Filter by search query if provided
            if (q != null && !q.isBlank()) {
                String searchLower = q.toLowerCase();
                boolean matches = false;
                if (lot.getBatchCode() != null && lot.getBatchCode().toLowerCase().contains(searchLower)) {
                    matches = true;
                }
                if (lot.getSupplyItem() != null && lot.getSupplyItem().getName() != null
                        && lot.getSupplyItem().getName().toLowerCase().contains(searchLower)) {
                    matches = true;
                }
                if (!matches)
                    continue;
            }

            BigDecimal onHand = stockMovementRepository.calculateOnHandQuantity(lot, warehouse, location);

            // Only include rows with positive on-hand
            if (onHand.compareTo(BigDecimal.ZERO) > 0) {
                OnHandRowResponse row = OnHandRowResponse.builder()
                        .warehouseId(warehouse.getId())
                        .warehouseName(warehouse.getName())
                        .locationId(location != null ? location.getId() : null)
                        .locationLabel(location != null ? buildLocationLabel(location) : "Any Location")
                        .supplyLotId(lot.getId())
                        .batchCode(lot.getBatchCode())
                        .supplyItemName(lot.getSupplyItem() != null ? lot.getSupplyItem().getName() : null)
                        .unit(lot.getSupplyItem() != null ? lot.getSupplyItem().getUnit() : null)
                        .expiryDate(lot.getExpiryDate())
                        .lotStatus(lot.getStatus())
                        .onHandQuantity(onHand)
                        .build();
                allRows.add(row);
            }
        }

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allRows.size());

        if (start >= allRows.size()) {
            return createEmptyPageResponse(pageable);
        }

        List<OnHandRowResponse> pagedRows = allRows.subList(start, end);

        PageResponse<OnHandRowResponse> response = new PageResponse<>();
        response.setItems(pagedRows);
        response.setPage(pageable.getPageNumber());
        response.setSize(pageable.getPageSize());
        response.setTotalElements(allRows.size());
        response.setTotalPages((int) Math.ceil((double) allRows.size() / pageable.getPageSize()));
        return response;
    }

    // ============================================
    // GET MOVEMENTS (Paginated History)
    // ============================================
    public PageResponse<StockMovementResponse> getMovements(Integer warehouseId, String type,
            LocalDate from, LocalDate to, Pageable pageable) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        ensureWarehouseOwnership(warehouse);

        StockMovementType movementType = null;
        if (type != null && !type.isBlank()) {
            movementType = StockMovementType.fromCode(type);
        }

        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.atTime(23, 59, 59) : null;

        Page<StockMovement> movementsPage = stockMovementRepository.findByWarehouseWithFilters(
                warehouse, movementType, fromDateTime, toDateTime, pageable);

        List<StockMovementResponse> items = movementsPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(movementsPage, items);
    }

    // ============================================
    // RECORD MOVEMENT (Enhanced with validations)
    // ============================================
    public StockMovementResponse recordMovement(RecordStockMovementRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        ensureWarehouseOwnership(warehouse);

        SupplyLot lot = supplyLotRepository.findById(request.getSupplyLotId())
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLY_LOT_NOT_FOUND));

        StockLocation location = null;
        if (request.getLocationId() != null) {
            location = stockLocationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
            if (!location.getWarehouse().getId().equals(warehouse.getId())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }

        StockMovementType type = StockMovementType.fromCode(request.getMovementType());
        if (type == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        BigDecimal quantity = request.getQuantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        // ========== VALIDATION: ADJUST requires note ==========
        if (type == StockMovementType.ADJUST) {
            if (request.getNote() == null || request.getNote().isBlank()) {
                throw new AppException(ErrorCode.ADJUST_NOTE_REQUIRED);
            }
        }

        // ========== VALIDATION: OUT requires season and lot status ==========
        if (type == StockMovementType.OUT) {
            if (request.getSeasonId() == null) {
                throw new AppException(ErrorCode.OUT_SEASON_REQUIRED);
            }
            if (!"IN_STOCK".equals(lot.getStatus())) {
                throw new AppException(ErrorCode.LOT_NOT_IN_STOCK);
            }
        }

        Season season = null;
        if (request.getSeasonId() != null) {
            season = seasonRepository.findById(request.getSeasonId())
                    .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        }

        var task = request.getTaskId() != null
                ? taskRepository.findById(request.getTaskId())
                        .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND))
                : null;

        if (task != null) {
            if (task.getSeason() == null) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            if (season != null && !task.getSeason().getId().equals(season.getId())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            if (season == null) {
                season = task.getSeason();
            }
        }

        // ========== VALIDATION: Season and warehouse must be same farm (for OUT)
        // ==========
        if (type == StockMovementType.OUT && season != null) {
            if (season.getPlot() == null || season.getPlot().getFarm() == null
                    || !season.getPlot().getFarm().getId().equals(warehouse.getFarm().getId())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }

        // ========== VALIDATION: Sufficient on-hand for OUT ==========
        if (type == StockMovementType.OUT) {
            BigDecimal onHand = stockMovementRepository.calculateOnHandQuantity(lot, warehouse, location);
            if (onHand.compareTo(quantity) < 0) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
            }
        }

        StockMovement movement = StockMovement.builder()
                .supplyLot(lot)
                .warehouse(warehouse)
                .location(location)
                .movementType(type)
                .quantity(quantity)
                .movementDate(LocalDateTime.now())
                .season(season)
                .task(task)
                .note(request.getNote())
                .build();

        StockMovement saved = stockMovementRepository.save(movement);
        return toResponse(saved);
    }

    // ============================================
    // GET ON-HAND QUANTITY (Simple endpoint)
    // ============================================
    public BigDecimal getOnHandQuantity(Integer supplyLotId, Integer warehouseId, Integer locationId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        ensureWarehouseOwnership(warehouse);

        SupplyLot lot = supplyLotRepository.findById(supplyLotId)
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLY_LOT_NOT_FOUND));

        StockLocation location = null;
        if (locationId != null) {
            location = stockLocationRepository.findById(locationId)
                    .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
        }

        return stockMovementRepository.calculateOnHandQuantity(lot, warehouse, location);
    }

    // ============================================
    // HELPER METHODS
    // ============================================
    private void ensureWarehouseOwnership(Warehouse warehouse) {
        Farm farm = warehouse.getFarm();
        if (farm == null) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        farmAccessService.assertCurrentUserCanAccessFarm(farm);
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

    private WarehouseResponse toWarehouseResponse(Warehouse warehouse) {
        return WarehouseResponse.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .type(warehouse.getType())
                .farmId(warehouse.getFarm() != null ? warehouse.getFarm().getId() : null)
                .farmName(warehouse.getFarm() != null ? warehouse.getFarm().getName() : null)
                .provinceId(warehouse.getProvince() != null ? warehouse.getProvince().getId() : null)
                .wardId(warehouse.getWard() != null ? warehouse.getWard().getId() : null)
                .build();
    }

    private StockLocationResponse toStockLocationResponse(StockLocation location) {
        return StockLocationResponse.builder()
                .id(location.getId())
                .warehouseId(location.getWarehouse() != null ? location.getWarehouse().getId() : null)
                .zone(location.getZone())
                .aisle(location.getAisle())
                .shelf(location.getShelf())
                .bin(location.getBin())
                .label(buildLocationLabel(location))
                .build();
    }

    private StockMovementResponse toResponse(StockMovement movement) {
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

    private PageResponse<OnHandRowResponse> createEmptyPageResponse(Pageable pageable) {
        PageResponse<OnHandRowResponse> response = new PageResponse<>();
        response.setItems(List.of());
        response.setPage(pageable.getPageNumber());
        response.setSize(pageable.getPageSize());
        response.setTotalElements(0);
        response.setTotalPages(0);
        return response;
    }
}
