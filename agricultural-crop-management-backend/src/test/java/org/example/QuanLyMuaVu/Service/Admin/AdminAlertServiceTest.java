package org.example.QuanLyMuaVu.Service.Admin;

import org.example.QuanLyMuaVu.DTO.Request.AlertSendRequest;
import org.example.QuanLyMuaVu.DTO.Response.AdminAlertResponse;
import org.example.QuanLyMuaVu.Entity.Alert;
import org.example.QuanLyMuaVu.Entity.Farm;
import org.example.QuanLyMuaVu.Entity.Notification;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Enums.AlertSeverity;
import org.example.QuanLyMuaVu.Enums.AlertStatus;
import org.example.QuanLyMuaVu.Enums.AlertType;
import org.example.QuanLyMuaVu.Repository.AlertRepository;
import org.example.QuanLyMuaVu.Repository.FarmRepository;
import org.example.QuanLyMuaVu.Repository.NotificationRepository;
import org.example.QuanLyMuaVu.Repository.StockMovementRepository;
import org.example.QuanLyMuaVu.Repository.StockMovementRepository.InventoryLotRiskProjection;
import org.example.QuanLyMuaVu.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private AdminAlertService adminAlertService;

    @Test
    void refreshAlerts_deduplicatesByFarmAndDay() {
        LocalDate today = LocalDate.now();
        InventoryLotRiskProjection expiredLot = new TestInventoryLotRiskProjection(1, "Farm A", 11, 101, "Urea",
                "LOT-1", today.minusDays(1), "kg", new BigDecimal("5"));
        InventoryLotRiskProjection expiringLot = new TestInventoryLotRiskProjection(1, "Farm A", 12, 102, "NPK",
                "LOT-2", today.plusDays(3), "kg", new BigDecimal("4"));

        when(stockMovementRepository.findOnHandLotsWithDetails(null, null))
                .thenReturn(List.of(expiredLot, expiringLot));

        Alert existingExpired = Alert.builder()
                .id(100L)
                .farmId(1)
                .type(AlertType.INVENTORY_EXPIRED)
                .status(AlertStatus.NEW)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        when(alertRepository.findFirstByFarmIdAndTypeAndCreatedAtBetween(eq(1), eq(AlertType.INVENTORY_EXPIRED), any(),
                any())).thenReturn(Optional.of(existingExpired));
        when(alertRepository.findFirstByFarmIdAndTypeAndCreatedAtBetween(eq(1), eq(AlertType.INVENTORY_EXPIRING), any(),
                any())).thenReturn(Optional.empty());

        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(farmRepository.findAllById(any())).thenReturn(List.of(Farm.builder().id(1).name("Farm A").build()));

        List<AdminAlertResponse> responses = adminAlertService.refreshAlerts(30);

        assertEquals(2, responses.size());
        assertNotNull(responses.stream().filter(r -> r.getType().equals("INVENTORY_EXPIRED")).findFirst().orElse(null));
        assertNotNull(responses.stream().filter(r -> r.getType().equals("INVENTORY_EXPIRING")).findFirst().orElse(null));
    }

    @Test
    void sendAlert_createsNotificationsAndSetsSent() {
        Alert alert = Alert.builder()
                .id(10L)
                .farmId(2)
                .type(AlertType.INVENTORY_EXPIRING)
                .severity(AlertSeverity.HIGH)
                .status(AlertStatus.NEW)
                .title("Inventory expiring soon")
                .message("2 lots expire soon.")
                .suggestedActionUrl("/admin/inventory?farmId=2&status=RISK&windowDays=30")
                .createdAt(LocalDateTime.now())
                .build();

        when(alertRepository.findById(10L)).thenReturn(Optional.of(alert));

        User owner = User.builder().id(7L).username("owner").build();
        Farm farm = Farm.builder().id(2).name("Farm B").user(owner).build();
        when(farmRepository.findById(2)).thenReturn(Optional.of(farm));
        when(userRepository.findAllById(List.of(7L))).thenReturn(List.of(owner));
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(farmRepository.findAllById(any())).thenReturn(List.of(farm));

        AlertSendRequest request = AlertSendRequest.builder()
                .channel("IN_APP")
                .recipientMode("ALL_FARMERS_IN_FARM")
                .build();

        AdminAlertResponse response = adminAlertService.sendAlert(10L, request);

        assertEquals("SENT", response.getStatus());
        verify(notificationRepository).saveAll(any());
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
