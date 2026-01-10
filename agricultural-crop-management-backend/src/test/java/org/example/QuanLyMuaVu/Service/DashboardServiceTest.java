package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.DTO.Response.DashboardOverviewResponse;
import org.example.QuanLyMuaVu.DTO.Response.TodayTaskResponse;
import org.example.QuanLyMuaVu.Entity.*;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.*;
import org.example.QuanLyMuaVu.Service.Dashboard.DashboardAlertsService;
import org.example.QuanLyMuaVu.Service.Dashboard.DashboardKpiService;
import org.example.QuanLyMuaVu.Util.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Functional tests for DashboardService.
 * 
 * Covers key operations: get overview, get today tasks, get plot status.
 */
@ExtendWith(MockitoExtension.class)
public class DashboardServiceTest {

        @Mock
        private CurrentUserService currentUserService;

        @Mock
        private FarmerOwnershipService ownershipService;

        @Mock
        private FarmRepository farmRepository;

        @Mock
        private PlotRepository plotRepository;

        @Mock
        private SeasonRepository seasonRepository;

        @Mock
        private DashboardTaskViewRepository dashboardTaskViewRepository;

        @Mock
        private IncidentRepository incidentRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private DashboardKpiService kpiService;

        @Mock
        private DashboardAlertsService alertsService;

        @InjectMocks
        private DashboardService dashboardService;

        private User testUser;
        private Season testSeason;
        private Farm testFarm;
        private Plot testPlot;

        @BeforeEach
        void setUp() {
                testUser = User.builder()
                                .id(1L)
                                .username("farmer")
                                .email("farmer@test.com")
                                .build();

                testFarm = Farm.builder()
                                .id(1)
                                .name("Test Farm")
                                .user(testUser)
                                .active(true)
                                .build();

                testPlot = Plot.builder()
                                .id(1)
                                .plotName("Test Plot")
                                .farm(testFarm)
                                .area(BigDecimal.valueOf(10))
                                .build();

                testSeason = Season.builder()
                                .id(1)
                                .seasonName("Spring 2024")
                                .plot(testPlot)
                                .status(SeasonStatus.ACTIVE)
                                .startDate(LocalDate.now().minusMonths(1))
                                .endDate(LocalDate.now().plusMonths(2))
                                .build();
        }

        @Test
        @DisplayName("GetOverview - Returns dashboard with season context")
        void getOverview_WithSeasonId_ReturnsDashboardOverview() {
                // Arrange
                when(currentUserService.getCurrentUserId()).thenReturn(1L);
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
                when(ownershipService.requireOwnedSeason(1)).thenReturn(testSeason);
                when(farmRepository.countByUserIdAndActiveTrue(1L)).thenReturn(2L);
                when(plotRepository.countByFarmUserId(1L)).thenReturn(5L);
                when(seasonRepository.countByStatusAndFarmUserId(any(SeasonStatus.class), eq(1L))).thenReturn(1L);
                when(kpiService.buildKpis(testSeason)).thenReturn(null);
                when(kpiService.buildExpenses(testSeason)).thenReturn(null);
                when(kpiService.buildHarvest(testSeason)).thenReturn(null);
                when(alertsService.buildAlerts(1L)).thenReturn(null);

                // Act
                DashboardOverviewResponse response = dashboardService.getOverview(1);

                // Assert
                assertNotNull(response);
                assertNotNull(response.getSeasonContext());
                assertEquals(1, response.getSeasonContext().getSeasonId());
                assertEquals("Spring 2024", response.getSeasonContext().getSeasonName());
                assertNotNull(response.getCounts());
                assertEquals(2, response.getCounts().getActiveFarms());
                assertEquals(5, response.getCounts().getActivePlots());
        }

        @Test
        @DisplayName("GetOverview - Throws USER_NOT_FOUND when user doesn't exist")
        void getOverview_WhenUserNotFound_ThrowsAppException() {
                // Arrange
                when(currentUserService.getCurrentUserId()).thenReturn(999L);
                when(userRepository.findById(999L)).thenReturn(Optional.empty());

                // Act & Assert
                AppException exception = assertThrows(AppException.class,
                                () -> dashboardService.getOverview(null));

                assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("GetTodayTasks - Returns paginated today tasks")
        void getTodayTasks_ReturnsPagedTasks() {
                // Arrange
                Pageable pageable = PageRequest.of(0, 10);

                // Create DashboardTaskView using setters (no builder)
                DashboardTaskView taskView = new DashboardTaskView();
                taskView.setTaskId(1);
                taskView.setTitle("Water Plants");
                taskView.setPlotName("Test Plot");
                taskView.setStatus(TaskStatus.PENDING);
                taskView.setDueDate(LocalDate.now());

                Page<DashboardTaskView> taskPage = new PageImpl<>(List.of(taskView), pageable, 1);

                when(currentUserService.getCurrentUserId()).thenReturn(1L);
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
                when(dashboardTaskViewRepository.findTodayTasks(eq(1L), any(), eq(LocalDate.now()), eq(pageable)))
                                .thenReturn(taskPage);

                // Act
                Page<TodayTaskResponse> result = dashboardService.getTodayTasks(null, pageable);

                // Assert
                assertNotNull(result);
                assertEquals(1, result.getTotalElements());
                assertEquals("Water Plants", result.getContent().get(0).getTitle());
        }

        @Test
        @DisplayName("GetUpcomingTasks - Returns tasks within N days")
        void getUpcomingTasks_ReturnsTasksWithinDays() {
                // Arrange
                DashboardTaskView taskView = new DashboardTaskView();
                taskView.setTaskId(1);
                taskView.setTitle("Apply Fertilizer");
                taskView.setPlotName("Field A");
                taskView.setStatus(TaskStatus.PENDING);
                taskView.setPlannedDate(LocalDate.now().plusDays(3));

                when(currentUserService.getCurrentUserId()).thenReturn(1L);
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
                when(dashboardTaskViewRepository.findUpcomingTasks(eq(1L), any(), any(), any(), any()))
                                .thenReturn(List.of(taskView));

                // Act
                List<TodayTaskResponse> result = dashboardService.getUpcomingTasks(7, null);

                // Assert
                assertNotNull(result);
                assertEquals(1, result.size());
                assertEquals("Apply Fertilizer", result.get(0).getTitle());
        }

        @Test
        @DisplayName("GetLowStock - Delegates to AlertsService")
        void getLowStock_DelegatesToAlertsService() {
                // Arrange
                when(alertsService.getLowStock(5)).thenReturn(List.of());

                // Act
                dashboardService.getLowStock(5);

                // Assert
                verify(alertsService).getLowStock(5);
        }
}
