package org.example.QuanLyMuaVu.controller;

import org.example.QuanLyMuaVu.Controller.Admin.AdminInventoryDashboardController;
import org.example.QuanLyMuaVu.DTO.Response.InventoryHealthResponse;
import org.example.QuanLyMuaVu.Repository.UserRepository;
import org.example.QuanLyMuaVu.Service.Admin.AdminInventoryHealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminInventoryDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminInventoryDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminInventoryHealthService adminInventoryHealthService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void inventoryHealth_returnsExpectedShape() throws Exception {
        InventoryHealthResponse response = InventoryHealthResponse.builder()
                .asOfDate(LocalDate.of(2026, 1, 19))
                .windowDays(30)
                .includeExpiring(true)
                .summary(InventoryHealthResponse.Summary.builder()
                        .expiredLots(1L)
                        .expiringLots(2L)
                        .qtyAtRisk(new BigDecimal("12"))
                        .unknownExpiryLots(0L)
                        .build())
                .farms(List.of(
                        InventoryHealthResponse.FarmRisk.builder()
                                .farmId(1)
                                .farmName("Farm A")
                                .expiredLots(1L)
                                .expiringLots(1L)
                                .qtyAtRisk(new BigDecimal("10"))
                                .topRiskLots(List.of(
                                        InventoryHealthResponse.RiskLot.builder()
                                                .lotId(100)
                                                .itemName("Urea")
                                                .expiryDate(LocalDate.of(2026, 1, 10))
                                                .onHand(new BigDecimal("5"))
                                                .status("EXPIRED")
                                                .build()))
                                .build()))
                .build();

        when(adminInventoryHealthService.getInventoryHealth(30, true, 5)).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/dashboard/inventory-health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.asOfDate").value("2026-01-19"))
                .andExpect(jsonPath("$.result.windowDays").value(30))
                .andExpect(jsonPath("$.result.includeExpiring").value(true))
                .andExpect(jsonPath("$.result.summary.expiredLots").value(1))
                .andExpect(jsonPath("$.result.farms[0].farmId").value(1))
                .andExpect(jsonPath("$.result.farms[0].topRiskLots[0].status").value("EXPIRED"));
    }
}
