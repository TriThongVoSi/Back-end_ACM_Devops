package org.example.QuanLyMuaVu.Service.Admin;

import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.DTO.Response.InventoryHealthResponse;
import org.example.QuanLyMuaVu.DTO.Response.InventoryRiskLotResponse;
import org.example.QuanLyMuaVu.Repository.StockMovementRepository;
import org.example.QuanLyMuaVu.Repository.StockMovementRepository.InventoryLotOnHandProjection;
import org.example.QuanLyMuaVu.Repository.StockMovementRepository.InventoryLotRiskProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInventoryHealthServiceTest {

    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private AdminInventoryHealthService adminInventoryHealthService;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
    }

    @Test
    void getInventoryHealth_classifiesExpiredAndExpiring() {
        when(stockMovementRepository.findOnHandLotsByFarm(null)).thenReturn(List.of(
                lot(1, "Farm A", 101, "Urea", today.minusDays(1), new BigDecimal("10")),
                lot(1, "Farm A", 102, "NPK", today.plusDays(10), new BigDecimal("5")),
                lot(1, "Farm A", 103, "DAP", today.plusDays(60), new BigDecimal("7"))
        ));

        InventoryHealthResponse response = adminInventoryHealthService.getInventoryHealth(30, true, 5);

        assertNotNull(response);
        assertEquals(1L, response.getSummary().getExpiredLots());
        assertEquals(1L, response.getSummary().getExpiringLots());
        assertEquals(0, response.getSummary().getQtyAtRisk().compareTo(new BigDecimal("15")));
        assertEquals(1, response.getFarms().size());
        assertEquals(1L, response.getFarms().get(0).getExpiredLots());
        assertEquals(1L, response.getFarms().get(0).getExpiringLots());
    }

    @Test
    void getInventoryHealth_excludesZeroOnHand() {
        when(stockMovementRepository.findOnHandLotsByFarm(null)).thenReturn(List.of(
                lot(1, "Farm A", 201, "Urea", today.minusDays(2), BigDecimal.ZERO)
        ));

        InventoryHealthResponse response = adminInventoryHealthService.getInventoryHealth(30, true, 5);

        assertEquals(0L, response.getSummary().getExpiredLots());
        assertEquals(0L, response.getSummary().getExpiringLots());
        assertTrue(response.getFarms().isEmpty());
    }

    @Test
    void getInventoryHealth_windowDaysBoundary() {
        when(stockMovementRepository.findOnHandLotsByFarm(null)).thenReturn(List.of(
                lot(2, "Farm B", 301, "Urea", today, new BigDecimal("3")),
                lot(2, "Farm B", 302, "NPK", today.plusDays(30), new BigDecimal("2")),
                lot(2, "Farm B", 303, "DAP", today.plusDays(31), new BigDecimal("4"))
        ));

        InventoryHealthResponse response = adminInventoryHealthService.getInventoryHealth(30, true, 5);

        assertEquals(0L, response.getSummary().getExpiredLots());
        assertEquals(2L, response.getSummary().getExpiringLots());
        assertEquals(0, response.getSummary().getQtyAtRisk().compareTo(new BigDecimal("5")));
    }

    @Test
    void getInventoryHealth_includeExpiringFalseReturnsExpiredOnly() {
        when(stockMovementRepository.findOnHandLotsByFarm(null)).thenReturn(List.of(
                lot(3, "Farm C", 401, "Urea", today.minusDays(3), new BigDecimal("6")),
                lot(3, "Farm C", 402, "NPK", today.plusDays(5), new BigDecimal("2"))
        ));

        InventoryHealthResponse response = adminInventoryHealthService.getInventoryHealth(30, false, 5);

        assertEquals(1L, response.getSummary().getExpiredLots());
        assertEquals(0L, response.getSummary().getExpiringLots());
        assertEquals(0, response.getSummary().getQtyAtRisk().compareTo(new BigDecimal("6")));
        assertEquals(1L, response.getFarms().get(0).getExpiredLots());
        assertEquals(0L, response.getFarms().get(0).getExpiringLots());
    }

    @Test
    void getInventoryHealth_sortsFarmsByExpiredThenExpiringThenQty() {
        when(stockMovementRepository.findOnHandLotsByFarm(null)).thenReturn(List.of(
                lot(1, "Farm A", 501, "Urea", today.minusDays(1), new BigDecimal("2")),
                lot(1, "Farm A", 502, "NPK", today.minusDays(2), new BigDecimal("3")),
                lot(2, "Farm B", 503, "DAP", today.minusDays(1), new BigDecimal("1")),
                lot(2, "Farm B", 504, "DAP", today.minusDays(2), new BigDecimal("1")),
                lot(2, "Farm B", 505, "DAP", today.plusDays(5), new BigDecimal("1")),
                lot(3, "Farm C", 506, "DAP", today.minusDays(1), new BigDecimal("10"))
        ));

        InventoryHealthResponse response = adminInventoryHealthService.getInventoryHealth(30, true, 5);

        assertEquals(3, response.getFarms().size());
        assertEquals(2, response.getFarms().get(0).getFarmId());
        assertEquals(1, response.getFarms().get(1).getFarmId());
        assertEquals(3, response.getFarms().get(2).getFarmId());
    }

    @Test
    void getRiskLots_filtersExpiredOnly() {
        when(stockMovementRepository.findOnHandLotsWithDetails(10, null)).thenReturn(List.of(
                riskLot(10, "Farm Z", 601, 1001, "Urea", "LOT-1", today.minusDays(2), "kg", new BigDecimal("4")),
                riskLot(10, "Farm Z", 602, 1002, "NPK", "LOT-2", today.plusDays(4), "kg", new BigDecimal("3"))
        ));

        PageResponse<InventoryRiskLotResponse> response = adminInventoryHealthService.getRiskLots(
                10, "EXPIRED", 30, 0, 10);

        assertEquals(1, response.getItems().size());
        assertEquals("EXPIRED", response.getItems().get(0).getStatus());
        assertTrue(response.getItems().get(0).getDaysToExpiry() < 0);
    }

    @Test
    void getRiskLots_includesUnknownExpiryAndLowStock() {
        when(stockMovementRepository.findOnHandLotsWithDetails(5, null)).thenReturn(List.of(
                riskLot(5, "Farm Q", 701, 2001, "Urea", "LOT-A", null, "kg", new BigDecimal("2")),
                riskLot(5, "Farm Q", 702, 2002, "DAP", "LOT-B", today.plusDays(120), "kg", new BigDecimal("3"))
        ));

        PageResponse<InventoryRiskLotResponse> response = adminInventoryHealthService.getRiskLots(
                5, "RISK", 30, 0, 10);

        assertEquals(2, response.getItems().size());
        assertTrue(response.getItems().stream().anyMatch(item -> "UNKNOWN_EXPIRY".equals(item.getStatus())));
        assertTrue(response.getItems().stream().anyMatch(item -> "LOW_STOCK".equals(item.getStatus())));
    }

    private InventoryLotOnHandProjection lot(Integer farmId, String farmName, Integer lotId, String itemName,
            LocalDate expiryDate, BigDecimal onHand) {
        return new TestInventoryLotOnHandProjection(farmId, farmName, lotId, itemName, expiryDate, onHand);
    }

    private InventoryLotRiskProjection riskLot(Integer farmId, String farmName, Integer lotId, Integer itemId,
            String itemName, String lotCode, LocalDate expiryDate, String unit, BigDecimal onHand) {
        return new TestInventoryLotRiskProjection(farmId, farmName, lotId, itemId, itemName, lotCode, expiryDate, unit,
                onHand);
    }

    private static class TestInventoryLotOnHandProjection implements InventoryLotOnHandProjection {
        private final Integer farmId;
        private final String farmName;
        private final Integer lotId;
        private final String itemName;
        private final LocalDate expiryDate;
        private final BigDecimal onHand;

        private TestInventoryLotOnHandProjection(Integer farmId, String farmName, Integer lotId, String itemName,
                LocalDate expiryDate, BigDecimal onHand) {
            this.farmId = farmId;
            this.farmName = farmName;
            this.lotId = lotId;
            this.itemName = itemName;
            this.expiryDate = expiryDate;
            this.onHand = onHand;
        }

        @Override
        public Integer getFarmId() {
            return farmId;
        }

        @Override
        public String getFarmName() {
            return farmName;
        }

        @Override
        public Integer getLotId() {
            return lotId;
        }

        @Override
        public String getItemName() {
            return itemName;
        }

        @Override
        public LocalDate getExpiryDate() {
            return expiryDate;
        }

        @Override
        public BigDecimal getOnHand() {
            return onHand;
        }
    }

    private static class TestInventoryLotRiskProjection implements InventoryLotRiskProjection {
        private final Integer farmId;
        private final String farmName;
        private final Integer lotId;
        private final Integer itemId;
        private final String itemName;
        private final String lotCode;
        private final LocalDate expiryDate;
        private final String unit;
        private final BigDecimal onHand;

        private TestInventoryLotRiskProjection(Integer farmId, String farmName, Integer lotId, Integer itemId,
                String itemName, String lotCode, LocalDate expiryDate, String unit, BigDecimal onHand) {
            this.farmId = farmId;
            this.farmName = farmName;
            this.lotId = lotId;
            this.itemId = itemId;
            this.itemName = itemName;
            this.lotCode = lotCode;
            this.expiryDate = expiryDate;
            this.unit = unit;
            this.onHand = onHand;
        }

        @Override
        public Integer getFarmId() {
            return farmId;
        }

        @Override
        public String getFarmName() {
            return farmName;
        }

        @Override
        public Integer getLotId() {
            return lotId;
        }

        @Override
        public Integer getItemId() {
            return itemId;
        }

        @Override
        public String getItemName() {
            return itemName;
        }

        @Override
        public String getLotCode() {
            return lotCode;
        }

        @Override
        public LocalDate getExpiryDate() {
            return expiryDate;
        }

        @Override
        public String getUnit() {
            return unit;
        }

        @Override
        public BigDecimal getOnHand() {
            return onHand;
        }
    }
}
