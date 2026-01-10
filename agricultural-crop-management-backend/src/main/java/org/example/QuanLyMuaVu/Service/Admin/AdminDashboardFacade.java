package org.example.QuanLyMuaVu.Service.Admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Response.DashboardStatsDTO;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.Repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Dashboard Facade
 * Aggregates statistics from multiple repositories for admin dashboard.
 * Uses existing entities and repositories only.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminDashboardFacade {

        private final UserRepository userRepository;
        private final FarmRepository farmRepository;
        private final PlotRepository plotRepository;
        private final SeasonRepository seasonRepository;
        private final DashboardRepository dashboardRepository;

        /**
         * Get system-wide dashboard statistics for admin
         * Returns data matching DashboardStatsDTO structure
         */
        public DashboardStatsDTO getDashboardStats() {
                log.info("Fetching admin dashboard stats");

                // KPI Summary
                DashboardStatsDTO.Summary summary = DashboardStatsDTO.Summary.builder()
                                .totalUsers(userRepository.count())
                                .totalFarms(farmRepository.count())
                                .totalPlots(plotRepository.count())
                                .totalSeasons(seasonRepository.count())
                                .build();

                // User Distribution by Role
                List<DashboardStatsDTO.UserRoleCount> userRoleCounts = dashboardRepository.countUsersByRole().stream()
                                .map(p -> DashboardStatsDTO.UserRoleCount.builder()
                                                .role(p.getRole())
                                                .total(p.getTotal())
                                                .build())
                                .collect(Collectors.toList());

                // User Distribution by Status
                List<DashboardStatsDTO.UserStatusCount> userStatusCounts = dashboardRepository.countUsersByStatus()
                                .stream()
                                .map(p -> DashboardStatsDTO.UserStatusCount.builder()
                                                .status(p.getStatus() != null ? p.getStatus().name() : "UNKNOWN")
                                                .total(p.getTotal())
                                                .build())
                                .collect(Collectors.toList());

                // Season Distribution by Status
                List<DashboardStatsDTO.SeasonStatusCount> seasonStatusCounts = dashboardRepository
                                .countSeasonsByStatus().stream()
                                .map(p -> DashboardStatsDTO.SeasonStatusCount.builder()
                                                .status(p.getStatus() != null ? p.getStatus().name() : "UNKNOWN")
                                                .total(p.getTotal())
                                                .build())
                                .collect(Collectors.toList());

                // Risky Seasons (top 10)
                List<DashboardStatsDTO.RiskySeason> riskySeasons = dashboardRepository
                                .findRiskySeasons(TaskStatus.OVERDUE, PageRequest.of(0, 10)).stream()
                                .filter(p -> (p.getIncidentCount() != null && p.getIncidentCount() > 0) ||
                                                (p.getOverdueTaskCount() != null && p.getOverdueTaskCount() > 0))
                                .map(p -> DashboardStatsDTO.RiskySeason.builder()
                                                .seasonId(p.getSeasonId())
                                                .seasonName(p.getSeasonName())
                                                .farmName(p.getFarmName())
                                                .plotName(p.getPlotName())
                                                .status(p.getStatus() != null ? p.getStatus().name() : "UNKNOWN")
                                                .incidentCount(p.getIncidentCount() != null ? p.getIncidentCount() : 0L)
                                                .overdueTaskCount(p.getOverdueTaskCount() != null
                                                                ? p.getOverdueTaskCount()
                                                                : 0L)
                                                .riskScore(p.getRiskScore() != null ? p.getRiskScore() : 0L)
                                                .build())
                                .collect(Collectors.toList());

                // Inventory Health (items expiring within 30 days)
                LocalDate today = LocalDate.now();
                LocalDate cutoff = today.plusDays(30);
                List<DashboardStatsDTO.InventoryHealth> inventoryHealth = dashboardRepository
                                .findInventoryHealth(today, cutoff).stream()
                                .map(p -> DashboardStatsDTO.InventoryHealth.builder()
                                                .farmId(p.getFarmId())
                                                .farmName(p.getFarmName())
                                                .expiredCount(p.getExpiredCount() != null ? p.getExpiredCount() : 0L)
                                                .expiringSoonCount(p.getExpiringSoonCount() != null
                                                                ? p.getExpiringSoonCount()
                                                                : 0L)
                                                .totalAtRisk(p.getTotalAtRisk() != null ? p.getTotalAtRisk() : 0L)
                                                .build())
                                .collect(Collectors.toList());

                return DashboardStatsDTO.builder()
                                .summary(summary)
                                .userRoleCounts(userRoleCounts)
                                .userStatusCounts(userStatusCounts)
                                .seasonStatusCounts(seasonStatusCounts)
                                .riskySeasons(riskySeasons)
                                .inventoryHealth(inventoryHealth)
                                .build();
        }
}
