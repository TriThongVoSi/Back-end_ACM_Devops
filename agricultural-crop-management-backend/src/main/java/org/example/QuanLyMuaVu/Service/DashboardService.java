package org.example.QuanLyMuaVu.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Response.*;
import org.example.QuanLyMuaVu.Entity.*;
import org.example.QuanLyMuaVu.Enums.IncidentStatus;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.*;
import org.example.QuanLyMuaVu.Service.Dashboard.DashboardAlertsService;
import org.example.QuanLyMuaVu.Service.Dashboard.DashboardKpiService;
import org.example.QuanLyMuaVu.Util.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dashboard Service - Orchestrates dashboard data aggregation.
 * Refactored to follow Single Responsibility Principle.
 * 
 * Responsibilities split into:
 * - DashboardService (this): Orchestration and general queries
 * - DashboardKpiService: KPI calculations
 * - DashboardAlertsService: Alert aggregation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final CurrentUserService currentUserService;
    private final FarmerOwnershipService ownershipService;
    private final FarmRepository farmRepository;
    private final PlotRepository plotRepository;
    private final SeasonRepository seasonRepository;
    private final DashboardTaskViewRepository dashboardTaskViewRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;

    // Delegated services (SRP compliance)
    private final DashboardKpiService kpiService;
    private final DashboardAlertsService alertsService;

    private static final List<TaskStatus> COMPLETED_STATUSES = List.of(TaskStatus.DONE, TaskStatus.CANCELLED);

    /**
     * Get dashboard overview with all aggregated metrics.
     */
    public DashboardOverviewResponse getOverview(Integer seasonId) {
        Long ownerId = currentUserService.getCurrentUserId();
        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Season season = resolveSeasonContext(seasonId, ownerId);

        return DashboardOverviewResponse.builder()
                .seasonContext(buildSeasonContext(season))
                .counts(buildCounts(ownerId))
                .kpis(kpiService.buildKpis(season))
                .expenses(kpiService.buildExpenses(season))
                .harvest(kpiService.buildHarvest(season))
                .alerts(alertsService.buildAlerts(ownerId))
                .build();
    }

    /**
     * Get today's tasks for dashboard table.
     */
    public Page<TodayTaskResponse> getTodayTasks(Integer seasonId, Pageable pageable) {
        Long ownerId = currentUserService.getCurrentUserId();
        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        LocalDate today = LocalDate.now();
        Page<DashboardTaskView> tasks = dashboardTaskViewRepository.findTodayTasks(
                user.getId(), seasonId, today, pageable);

        return tasks.map(this::mapToTodayTaskResponse);
    }

    /**
     * Get upcoming tasks within N days.
     */
    public List<TodayTaskResponse> getUpcomingTasks(int days, Integer seasonId) {
        Long ownerId = currentUserService.getCurrentUserId();
        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        LocalDate today = LocalDate.now();
        LocalDate untilDate = today.plusDays(days);

        List<DashboardTaskView> tasks = dashboardTaskViewRepository.findUpcomingTasks(
                user.getId(), seasonId, today, untilDate, COMPLETED_STATUSES);

        return tasks.stream()
                .map(this::mapToTodayTaskResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get plot status list for owner's plots.
     */
    public List<PlotStatusResponse> getPlotStatus(Integer seasonId) {
        Long ownerId = currentUserService.getCurrentUserId();
        List<Plot> plots = plotRepository.findAllByFarmUserId(ownerId);

        return plots.stream()
                .map(this::mapToPlotStatusResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get low stock alerts - delegates to AlertsService.
     */
    public List<LowStockAlertResponse> getLowStock(int limit) {
        return alertsService.getLowStock(limit);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private Season resolveSeasonContext(Integer seasonId, Long ownerId) {
        if (seasonId != null) {
            return ownershipService.requireOwnedSeason(seasonId);
        }

        List<Season> activeSeasons = seasonRepository.findActiveSeasonsByUserIdOrderByStartDateDesc(ownerId);
        if (!activeSeasons.isEmpty()) {
            return activeSeasons.get(0);
        }

        List<Season> allSeasons = seasonRepository.findAllByFarmUserId(ownerId);
        if (!allSeasons.isEmpty()) {
            return allSeasons.stream()
                    .max(Comparator.comparing(Season::getStartDate))
                    .orElse(null);
        }

        return null;
    }

    private DashboardOverviewResponse.SeasonContext buildSeasonContext(Season season) {
        if (season == null) {
            return null;
        }
        return DashboardOverviewResponse.SeasonContext.builder()
                .seasonId(season.getId())
                .seasonName(season.getSeasonName())
                .startDate(season.getStartDate())
                .endDate(season.getEndDate())
                .plannedHarvestDate(season.getPlannedHarvestDate())
                .build();
    }

    private DashboardOverviewResponse.Counts buildCounts(Long ownerId) {
        long activeFarms = farmRepository.countByUserIdAndActiveTrue(ownerId);
        long activePlots = plotRepository.countByFarmUserId(ownerId);

        Map<String, Integer> seasonsByStatus = new LinkedHashMap<>();
        for (SeasonStatus status : SeasonStatus.values()) {
            long count = seasonRepository.countByStatusAndFarmUserId(status, ownerId);
            seasonsByStatus.put(status.name(), (int) count);
        }

        return DashboardOverviewResponse.Counts.builder()
                .activeFarms((int) activeFarms)
                .activePlots((int) activePlots)
                .seasonsByStatus(seasonsByStatus)
                .build();
    }

    private TodayTaskResponse mapToTodayTaskResponse(DashboardTaskView task) {
        String plotName = task.getPlotName() != null ? task.getPlotName() : "";
        String type = inferTaskType(task.getTitle(), task.getDescription());
        LocalDate dueDate = task.getDueDate() != null ? task.getDueDate() : task.getPlannedDate();
        return TodayTaskResponse.builder()
                .taskId(task.getTaskId())
                .title(task.getTitle())
                .plotName(plotName)
                .type(type)
                .assigneeName(task.getAssigneeName() != null ? task.getAssigneeName() : "")
                .dueDate(dueDate)
                .status(task.getStatus() != null ? task.getStatus().name() : "")
                .build();
    }

    private String inferTaskType(String title, String description) {
        String text = String.format("%s %s",
                title != null ? title : "",
                description != null ? description : "").toLowerCase();
        if (text.contains("irrigat") || text.contains("water"))
            return "irrigation";
        if (text.contains("fertil") || text.contains("npk"))
            return "fertilizing";
        if (text.contains("spray") || text.contains("pest") || text.contains("insect"))
            return "spraying";
        if (text.contains("harvest") || text.contains("collect"))
            return "harvesting";
        if (text.contains("inspect") || text.contains("scout"))
            return "scouting";
        return "scouting";
    }

    private PlotStatusResponse mapToPlotStatusResponse(Plot plot) {
        String cropName = "N/A";
        String stage = "N/A";
        String health = "HEALTHY";

        List<Season> seasons = seasonRepository.findAllByPlot_Id(plot.getId());
        if (!seasons.isEmpty()) {
            Season latestSeason = seasons.stream()
                    .max(Comparator.comparing(Season::getStartDate))
                    .orElse(null);
            if (latestSeason != null) {
                if (latestSeason.getCrop() != null) {
                    cropName = latestSeason.getCrop().getCropName();
                }
                stage = latestSeason.getStatus() != null ? latestSeason.getStatus().name() : "N/A";

                List<Incident> incidents = incidentRepository.findAllBySeason(latestSeason);
                long openCount = incidents.stream()
                        .filter(i -> i.getStatus() == IncidentStatus.OPEN
                                || i.getStatus() == IncidentStatus.IN_PROGRESS)
                        .count();
                if (openCount > 2) {
                    health = "CRITICAL";
                } else if (openCount > 0) {
                    health = "WARNING";
                }
            }
        }

        return PlotStatusResponse.builder()
                .plotId(plot.getId())
                .plotName(plot.getPlotName())
                .areaHa(plot.getArea())
                .cropName(cropName)
                .stage(stage)
                .health(health)
                .build();
    }
}
