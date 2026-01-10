package org.example.QuanLyMuaVu.Service.Dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Response.DashboardOverviewResponse;
import org.example.QuanLyMuaVu.DTO.Response.LowStockAlertResponse;
import org.example.QuanLyMuaVu.Entity.Farm;
import org.example.QuanLyMuaVu.Entity.SupplyLot;
import org.example.QuanLyMuaVu.Entity.Warehouse;
import org.example.QuanLyMuaVu.Enums.IncidentStatus;
import org.example.QuanLyMuaVu.Repository.IncidentRepository;
import org.example.QuanLyMuaVu.Repository.StockMovementRepository;
import org.example.QuanLyMuaVu.Repository.SupplyLotRepository;
import org.example.QuanLyMuaVu.Repository.WarehouseRepository;
import org.example.QuanLyMuaVu.Service.FarmerOwnershipService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for Dashboard alerts aggregation.
 * Single Responsibility: Alert computation and low stock detection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardAlertsService {

    private final FarmerOwnershipService ownershipService;
    private final IncidentRepository incidentRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockMovementRepository stockMovementRepository;
    private final SupplyLotRepository supplyLotRepository;

    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final List<IncidentStatus> OPEN_STATUSES = List.of(
            IncidentStatus.OPEN, IncidentStatus.IN_PROGRESS);

    /**
     * Build alerts summary for owner.
     */
    public DashboardOverviewResponse.Alerts buildAlerts(Long ownerId) {
        // Open incidents
        long openIncidents = incidentRepository.countByFarmUserIdAndStatusIn(ownerId, OPEN_STATUSES);

        // Expiring lots (within 30 days)
        int expiringLots = countExpiringLots();

        // Low stock count
        int lowStockCount = getLowStock(100).size();

        return DashboardOverviewResponse.Alerts.builder()
                .openIncidents((int) openIncidents)
                .expiringLots(expiringLots)
                .lowStockItems(lowStockCount)
                .build();
    }

    /**
     * Get low stock alerts.
     */
    public List<LowStockAlertResponse> getLowStock(int limit) {
        List<Farm> farms = ownershipService.getOwnedFarms();
        if (farms.isEmpty()) {
            return List.of();
        }

        List<LowStockAlertResponse> lowStockItems = new ArrayList<>();

        for (Farm farm : farms) {
            List<Warehouse> warehouses = warehouseRepository.findAllByFarm(farm);
            for (Warehouse warehouse : warehouses) {
                List<Integer> lotIds = stockMovementRepository.findDistinctSupplyLotIdsByWarehouse(warehouse, null);
                for (Integer lotId : lotIds) {
                    SupplyLot lot = supplyLotRepository.findById(lotId).orElse(null);
                    if (lot == null)
                        continue;

                    BigDecimal onHand = stockMovementRepository.calculateOnHandQuantity(lot, warehouse, null);
                    if (onHand != null && onHand.compareTo(BigDecimal.valueOf(LOW_STOCK_THRESHOLD)) <= 0) {
                        lowStockItems.add(LowStockAlertResponse.builder()
                                .supplyLotId(lot.getId())
                                .batchCode(lot.getBatchCode())
                                .itemName(lot.getSupplyItem() != null ? lot.getSupplyItem().getName() : "Unknown")
                                .warehouseName(warehouse.getName())
                                .locationLabel("")
                                .onHand(onHand)
                                .unit(lot.getSupplyItem() != null ? lot.getSupplyItem().getUnit() : "unit")
                                .build());
                    }

                    if (lowStockItems.size() >= limit)
                        break;
                }
                if (lowStockItems.size() >= limit)
                    break;
            }
            if (lowStockItems.size() >= limit)
                break;
        }

        return lowStockItems;
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private int countExpiringLots() {
        int expiringLots = 0;
        LocalDate expiryThreshold = LocalDate.now().plusDays(30);
        List<Farm> farms = ownershipService.getOwnedFarms();

        for (Farm farm : farms) {
            List<Warehouse> warehouses = warehouseRepository.findAllByFarm(farm);
            for (Warehouse warehouse : warehouses) {
                List<Integer> lotIds = stockMovementRepository.findDistinctSupplyLotIdsByWarehouse(warehouse, null);
                for (Integer lotId : lotIds) {
                    SupplyLot lot = supplyLotRepository.findById(lotId).orElse(null);
                    if (lot != null && lot.getExpiryDate() != null
                            && !lot.getExpiryDate().isAfter(expiryThreshold)) {
                        expiringLots++;
                    }
                }
            }
        }
        return expiringLots;
    }
}
