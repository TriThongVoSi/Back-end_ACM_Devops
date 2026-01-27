package org.example.QuanLyMuaVu.Service.Admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.DTO.Response.InventoryFarmOptionResponse;
import org.example.QuanLyMuaVu.DTO.Response.InventoryHealthResponse;
import org.example.QuanLyMuaVu.DTO.Response.InventoryLotBalanceResponse;
import org.example.QuanLyMuaVu.DTO.Response.InventoryLotDetailResponse;
import org.example.QuanLyMuaVu.DTO.Response.InventoryLotMovementResponse;
import org.example.QuanLyMuaVu.DTO.Response.InventoryOptionsResponse;
import org.example.QuanLyMuaVu.DTO.Response.InventoryRiskLotResponse;
import org.example.QuanLyMuaVu.Entity.InventoryBalance;
import org.example.QuanLyMuaVu.Entity.StockLocation;
import org.example.QuanLyMuaVu.Entity.StockMovement;
import org.example.QuanLyMuaVu.Entity.SupplyLot;
import org.example.QuanLyMuaVu.Entity.Warehouse;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.FarmRepository;
import org.example.QuanLyMuaVu.Repository.InventoryBalanceRepository;
import org.example.QuanLyMuaVu.Repository.StockMovementRepository;
import org.example.QuanLyMuaVu.Repository.StockMovementRepository.InventoryLotOnHandProjection;
import org.example.QuanLyMuaVu.Repository.StockMovementRepository.InventoryLotRiskProjection;
import org.example.QuanLyMuaVu.Repository.SupplyLotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminInventoryHealthService {

    private static final BigDecimal DEFAULT_LOW_STOCK_THRESHOLD = new BigDecimal("5");

    private final StockMovementRepository stockMovementRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final SupplyLotRepository supplyLotRepository;
    private final FarmRepository farmRepository;

    public InventoryHealthResponse getInventoryHealth(Integer windowDays, Boolean includeExpiring, Integer limit) {
        int safeWindowDays = windowDays != null ? windowDays : 30;
        boolean includeExpiringFlag = includeExpiring != null ? includeExpiring : true;
        int safeLimit = limit != null ? limit : 5;

        if (safeWindowDays < 0 || safeLimit < 0) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        LocalDate today = LocalDate.now();
        List<InventoryLotOnHandProjection> lots = stockMovementRepository.findOnHandLotsByFarm(null);

        Map<Integer, FarmAccumulator> farmMap = new HashMap<>();
        long expiredLots = 0L;
        long expiringLots = 0L;
        long unknownExpiryLots = 0L;
        BigDecimal qtyAtRisk = BigDecimal.ZERO;

        for (InventoryLotOnHandProjection lot : lots) {
            BigDecimal onHand = lot.getOnHand() != null ? lot.getOnHand() : BigDecimal.ZERO;
            if (onHand.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            LocalDate expiryDate = lot.getExpiryDate();
            if (expiryDate == null) {
                unknownExpiryLots++;
                continue;
            }

            long daysToExpiry = ChronoUnit.DAYS.between(today, expiryDate);
            RiskStatus status = classify(daysToExpiry, safeWindowDays);
            if (status == RiskStatus.HEALTHY) {
                continue;
            }
            if (!includeExpiringFlag && status == RiskStatus.EXPIRING_SOON) {
                continue;
            }

            FarmAccumulator acc = farmMap.computeIfAbsent(lot.getFarmId(),
                    id -> new FarmAccumulator(lot.getFarmId(), lot.getFarmName()));

            if (status == RiskStatus.EXPIRED) {
                acc.expiredLots++;
                expiredLots++;
            } else {
                acc.expiringLots++;
                expiringLots++;
            }

            acc.qtyAtRisk = acc.qtyAtRisk.add(onHand);
            qtyAtRisk = qtyAtRisk.add(onHand);
            acc.riskLots.add(new RiskLotEntry(
                    lot.getLotId(),
                    lot.getItemName(),
                    expiryDate,
                    onHand,
                    status));
        }

        List<InventoryHealthResponse.FarmRisk> farms = farmMap.values().stream()
                .sorted(farmComparator())
                .limit(safeLimit)
                .map(acc -> InventoryHealthResponse.FarmRisk.builder()
                        .farmId(acc.farmId)
                        .farmName(acc.farmName)
                        .expiredLots(acc.expiredLots)
                        .expiringLots(acc.expiringLots)
                        .qtyAtRisk(acc.qtyAtRisk)
                        .topRiskLots(acc.riskLots.stream()
                                .sorted(riskLotComparator())
                                .limit(2)
                                .map(entry -> InventoryHealthResponse.RiskLot.builder()
                                        .lotId(entry.lotId)
                                        .itemName(entry.itemName)
                                        .expiryDate(entry.expiryDate)
                                        .onHand(entry.onHand)
                                        .status(entry.status == RiskStatus.EXPIRED
                                                ? "EXPIRED"
                                                : "EXPIRING_SOON")
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        return InventoryHealthResponse.builder()
                .asOfDate(today)
                .windowDays(safeWindowDays)
                .includeExpiring(includeExpiringFlag)
                .summary(InventoryHealthResponse.Summary.builder()
                        .expiredLots(expiredLots)
                        .expiringLots(expiringLots)
                        .qtyAtRisk(qtyAtRisk)
                        .unknownExpiryLots(unknownExpiryLots)
                        .build())
                .farms(farms)
                .build();
    }

    public PageResponse<InventoryRiskLotResponse> getRiskLots(Integer farmId, String status, Integer windowDays,
            int page, int limit) {
        return getRiskLots(farmId, status, windowDays, null, null, null, page, limit);
    }

    public PageResponse<InventoryRiskLotResponse> getRiskLots(Integer farmId, String status, Integer windowDays,
            String q, String sort, BigDecimal lowStockThreshold, int page, int limit) {
        if (page < 0 || limit <= 0) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        int safeWindowDays = windowDays != null ? windowDays : 30;
        if (safeWindowDays < 0) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        BigDecimal safeLowStockThreshold = lowStockThreshold != null ? lowStockThreshold : DEFAULT_LOW_STOCK_THRESHOLD;
        if (safeLowStockThreshold.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        RiskFilter filter = RiskFilter.from(status);
        SortOption sortOption = SortOption.from(sort);
        LocalDate today = LocalDate.now();
        String searchQuery = StringUtils.hasText(q) ? q.trim() : null;
        List<InventoryLotRiskProjection> lots = stockMovementRepository.findOnHandLotsWithDetails(farmId, searchQuery);
        List<InventoryRiskLotResponse> filtered = new ArrayList<>();

        for (InventoryLotRiskProjection lot : lots) {
            BigDecimal onHand = lot.getOnHand() != null ? lot.getOnHand() : BigDecimal.ZERO;
            if (onHand.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            LocalDate expiryDate = lot.getExpiryDate();
            Long daysToExpiry = expiryDate != null ? ChronoUnit.DAYS.between(today, expiryDate) : null;
            InventoryLotStatus statusValue = classifyLot(expiryDate, daysToExpiry, onHand, safeWindowDays,
                    safeLowStockThreshold);

            if (!matchesFilter(filter, statusValue)) {
                continue;
            }

            filtered.add(InventoryRiskLotResponse.builder()
                    .lotId(lot.getLotId())
                    .itemId(lot.getItemId())
                    .itemName(lot.getItemName())
                    .lotCode(lot.getLotCode())
                    .farmId(lot.getFarmId())
                    .farmName(lot.getFarmName())
                    .expiryDate(expiryDate)
                    .onHand(onHand)
                    .daysToExpiry(daysToExpiry)
                    .status(statusValue.name())
                    .unit(lot.getUnit())
                    .unitCost(null)
                    .build());
        }

        filtered.sort(buildSortComparator(sortOption));

        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / limit);
        int start = page * limit;
        int end = Math.min(start + limit, totalElements);

        List<InventoryRiskLotResponse> items = start >= totalElements
                ? List.of()
                : filtered.subList(start, end);

        PageResponse<InventoryRiskLotResponse> response = new PageResponse<>();
        response.setItems(items);
        response.setPage(page);
        response.setSize(limit);
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);
        return response;
    }

    public InventoryLotDetailResponse getLotDetail(Integer lotId) {
        if (lotId == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        SupplyLot lot = supplyLotRepository.findByIdWithDetails(lotId)
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLY_LOT_NOT_FOUND));

        BigDecimal onHandTotal = inventoryBalanceRepository.sumQuantityBySupplyLotId(lotId);
        List<InventoryLotBalanceResponse> balances = inventoryBalanceRepository.findBySupplyLotIdWithDetails(lotId)
                .stream()
                .map(this::toBalanceResponse)
                .collect(Collectors.toList());

        return InventoryLotDetailResponse.builder()
                .lotId(lot.getId())
                .itemId(lot.getSupplyItem() != null ? lot.getSupplyItem().getId() : null)
                .itemName(lot.getSupplyItem() != null ? lot.getSupplyItem().getName() : null)
                .lotCode(lot.getBatchCode())
                .unit(lot.getSupplyItem() != null ? lot.getSupplyItem().getUnit() : null)
                .supplierName(lot.getSupplier() != null ? lot.getSupplier().getName() : null)
                .expiryDate(lot.getExpiryDate())
                .status(lot.getStatus())
                .onHandTotal(onHandTotal)
                .balances(balances)
                .build();
    }

    public List<InventoryLotMovementResponse> getLotMovements(Integer lotId) {
        if (lotId == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        List<StockMovement> movements = stockMovementRepository.findBySupplyLot_IdOrderByMovementDateDesc(lotId);
        return movements.stream()
                .map(this::toMovementResponse)
                .collect(Collectors.toList());
    }

    public InventoryOptionsResponse getInventoryOptions() {
        List<InventoryFarmOptionResponse> farms = farmRepository.findAll().stream()
                .map(farm -> InventoryFarmOptionResponse.builder()
                        .id(farm.getId())
                        .name(farm.getName())
                        .build())
                .collect(Collectors.toList());

        return InventoryOptionsResponse.builder()
                .farms(farms)
                .categories(List.of())
                .build();
    }

    private RiskStatus classify(long daysToExpiry, int windowDays) {
        if (daysToExpiry < 0) {
            return RiskStatus.EXPIRED;
        }
        if (daysToExpiry <= windowDays) {
            return RiskStatus.EXPIRING_SOON;
        }
        return RiskStatus.HEALTHY;
    }

    private InventoryLotStatus classifyLot(LocalDate expiryDate, Long daysToExpiry, BigDecimal onHand, int windowDays,
            BigDecimal lowStockThreshold) {
        if (expiryDate == null) {
            return InventoryLotStatus.UNKNOWN_EXPIRY;
        }
        if (daysToExpiry != null && daysToExpiry < 0) {
            return InventoryLotStatus.EXPIRED;
        }
        if (daysToExpiry != null && daysToExpiry <= windowDays) {
            return InventoryLotStatus.EXPIRING;
        }
        if (onHand.compareTo(lowStockThreshold) <= 0) {
            return InventoryLotStatus.LOW_STOCK;
        }
        return InventoryLotStatus.HEALTHY;
    }

    private boolean matchesFilter(RiskFilter filter, InventoryLotStatus statusValue) {
        return switch (filter) {
            case ALL -> true;
            case RISK -> statusValue != InventoryLotStatus.HEALTHY;
            case EXPIRED -> statusValue == InventoryLotStatus.EXPIRED;
            case EXPIRING -> statusValue == InventoryLotStatus.EXPIRING;
            case LOW_STOCK -> statusValue == InventoryLotStatus.LOW_STOCK;
            case UNKNOWN_EXPIRY -> statusValue == InventoryLotStatus.UNKNOWN_EXPIRY;
        };
    }

    private Comparator<InventoryRiskLotResponse> buildSortComparator(SortOption sortOption) {
        return switch (sortOption) {
            case EXPIRY_ASC -> Comparator
                    .comparing(InventoryRiskLotResponse::getExpiryDate,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(InventoryRiskLotResponse::getOnHand, Comparator.reverseOrder());
            case EXPIRY_DESC -> Comparator
                    .comparing(InventoryRiskLotResponse::getExpiryDate,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(InventoryRiskLotResponse::getOnHand, Comparator.reverseOrder());
            case ON_HAND_DESC -> Comparator
                    .comparing(InventoryRiskLotResponse::getOnHand, Comparator.reverseOrder())
                    .thenComparing(InventoryRiskLotResponse::getExpiryDate,
                            Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator
                    .comparingInt((InventoryRiskLotResponse entry) -> statusRank(entry.getStatus()))
                    .thenComparing(InventoryRiskLotResponse::getExpiryDate,
                            Comparator.nullsLast(Comparator.naturalOrder()));
        };
    }

    private int statusRank(String status) {
        if ("EXPIRED".equals(status)) {
            return 0;
        }
        if ("EXPIRING".equals(status)) {
            return 1;
        }
        if ("LOW_STOCK".equals(status)) {
            return 2;
        }
        if ("UNKNOWN_EXPIRY".equals(status)) {
            return 3;
        }
        return 4;
    }

    private Comparator<FarmAccumulator> farmComparator() {
        return Comparator
                .comparingLong((FarmAccumulator acc) -> acc.expiredLots)
                .reversed()
                .thenComparing(Comparator.comparingLong((FarmAccumulator acc) -> acc.expiringLots).reversed())
                .thenComparing((a, b) -> b.qtyAtRisk.compareTo(a.qtyAtRisk));
    }

    private Comparator<RiskLotEntry> riskLotComparator() {
        return Comparator.<RiskLotEntry>comparingInt(entry -> entry.status.rank)
                .thenComparing(entry -> entry.expiryDate)
                .thenComparing(entry -> entry.onHand, Comparator.reverseOrder());
    }

    private enum RiskStatus {
        EXPIRED(0),
        EXPIRING_SOON(1),
        HEALTHY(2);

        private final int rank;

        RiskStatus(int rank) {
            this.rank = rank;
        }
    }

    private enum RiskFilter {
        EXPIRED,
        EXPIRING,
        LOW_STOCK,
        UNKNOWN_EXPIRY,
        RISK,
        ALL;

        static RiskFilter from(String value) {
            if (value == null || value.isBlank()) {
                return ALL;
            }

            String normalized = value.trim().toUpperCase();
            return switch (normalized) {
                case "EXPIRED" -> EXPIRED;
                case "EXPIRING", "EXPIRING_SOON" -> EXPIRING;
                case "LOW_STOCK" -> LOW_STOCK;
                case "UNKNOWN_EXPIRY" -> UNKNOWN_EXPIRY;
                case "RISK" -> RISK;
                case "ALL", "ALL_RISK" -> ALL;
                default -> throw new AppException(ErrorCode.BAD_REQUEST);
            };
        }
    }

    private enum SortOption {
        EXPIRY_ASC,
        EXPIRY_DESC,
        ON_HAND_DESC,
        DEFAULT;

        static SortOption from(String value) {
            if (value == null || value.isBlank()) {
                return DEFAULT;
            }
            String normalized = value.trim().toUpperCase();
            return switch (normalized) {
                case "EXPIRY_ASC" -> EXPIRY_ASC;
                case "EXPIRY_DESC" -> EXPIRY_DESC;
                case "ONHAND_DESC", "ON_HAND_DESC" -> ON_HAND_DESC;
                default -> DEFAULT;
            };
        }
    }

    private enum InventoryLotStatus {
        EXPIRED,
        EXPIRING,
        LOW_STOCK,
        UNKNOWN_EXPIRY,
        HEALTHY;
    }

    private InventoryLotBalanceResponse toBalanceResponse(InventoryBalance balance) {
        Warehouse warehouse = balance.getWarehouse();
        return InventoryLotBalanceResponse.builder()
                .warehouseId(warehouse != null ? warehouse.getId() : null)
                .warehouseName(warehouse != null ? warehouse.getName() : null)
                .farmId(warehouse != null && warehouse.getFarm() != null ? warehouse.getFarm().getId() : null)
                .farmName(warehouse != null && warehouse.getFarm() != null ? warehouse.getFarm().getName() : null)
                .locationId(balance.getLocation() != null ? balance.getLocation().getId() : null)
                .locationLabel(buildLocationLabel(balance.getLocation()))
                .quantity(balance.getQuantity())
                .build();
    }

    private String buildLocationLabel(StockLocation location) {
        if (location == null) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(location.getZone())) {
            parts.add("Zone " + location.getZone());
        }
        if (StringUtils.hasText(location.getAisle())) {
            parts.add("Aisle " + location.getAisle());
        }
        if (StringUtils.hasText(location.getShelf())) {
            parts.add("Shelf " + location.getShelf());
        }
        if (StringUtils.hasText(location.getBin())) {
            parts.add("Bin " + location.getBin());
        }
        return parts.isEmpty() ? null : String.join(" - ", parts);
    }

    private InventoryLotMovementResponse toMovementResponse(StockMovement movement) {
        return InventoryLotMovementResponse.builder()
                .movementId(movement.getId())
                .movementType(movement.getMovementType() != null ? movement.getMovementType().name() : null)
                .quantity(movement.getQuantity())
                .movementDate(movement.getMovementDate())
                .reference(buildMovementReference(movement))
                .note(movement.getNote())
                .build();
    }

    private String buildMovementReference(StockMovement movement) {
        if (movement.getSeason() != null) {
            return "Season " + movement.getSeason().getId();
        }
        if (movement.getTask() != null) {
            return "Task " + movement.getTask().getId();
        }
        if (movement.getWarehouse() != null) {
            return "Warehouse " + movement.getWarehouse().getName();
        }
        return null;
    }

    private static class FarmAccumulator {
        private final Integer farmId;
        private final String farmName;
        private long expiredLots;
        private long expiringLots;
        private BigDecimal qtyAtRisk = BigDecimal.ZERO;
        private final List<RiskLotEntry> riskLots = new ArrayList<>();

        private FarmAccumulator(Integer farmId, String farmName) {
            this.farmId = farmId;
            this.farmName = farmName;
        }
    }

    private static class RiskLotEntry {
        private final Integer lotId;
        private final String itemName;
        private final LocalDate expiryDate;
        private final BigDecimal onHand;
        private final RiskStatus status;

        private RiskLotEntry(Integer lotId, String itemName, LocalDate expiryDate, BigDecimal onHand,
                RiskStatus status) {
            this.lotId = lotId;
            this.itemName = itemName;
            this.expiryDate = expiryDate;
            this.onHand = onHand;
            this.status = status;
        }
    }
}
