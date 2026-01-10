package org.example.QuanLyMuaVu.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.DTO.Request.*;
import org.example.QuanLyMuaVu.DTO.Response.SeasonMinimalResponse;
import org.example.QuanLyMuaVu.DTO.Response.TaskResponse;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Entity.Task;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.SeasonRepository;
import org.example.QuanLyMuaVu.Repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Service for Tasks Workspace - user-scoped task management.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TaskWorkspaceService {

    TaskRepository taskRepository;
    SeasonRepository seasonRepository;
    FarmAccessService farmAccessService;

    /**
     * Create a new task for the current user.
     */
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        User currentUser = farmAccessService.getCurrentUser();

        // Validate dates
        validateTaskDates(request.getPlannedDate(), request.getDueDate(), null, null);

        // Validate season ownership if seasonId is provided
        Season season = null;
        if (request.getSeasonId() != null) {
            season = seasonRepository.findById(request.getSeasonId())
                    .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
            farmAccessService.assertCurrentUserCanAccessSeason(season);
        }

        // Build task
        Task task = Task.builder()
                .user(currentUser)
                .season(season)
                .title(request.getTitle())
                .description(request.getDescription())
                .plannedDate(request.getPlannedDate())
                .dueDate(request.getDueDate())
                .notes(request.getNotes())
                .status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        // Check if task should be OVERDUE immediately
        if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())) {
            task.setStatus(TaskStatus.OVERDUE);
        }

        task = taskRepository.save(task);
        log.info("Created task {} for user {}", task.getId(), currentUser.getId());

        return mapToResponse(task);
    }

    /**
     * List tasks for current user with filters and pagination.
     */
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> listTasks(
            TaskStatus status,
            Integer seasonId,
            String searchQuery,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection) {
        User currentUser = farmAccessService.getCurrentUser();

        // Auto-update overdue tasks before listing
        updateOverdueTasksForUser(currentUser);

        // Build pagination
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy != null ? sortBy : "createdAt");
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20, sort);

        // Execute query with filters
        Page<Task> taskPage = taskRepository.findByUserWithFilters(
                currentUser,
                status,
                seasonId,
                searchQuery,
                pageable);

        List<TaskResponse> responses = taskPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        PageResponse<TaskResponse> response = new PageResponse<>();
        response.setItems(responses);
        response.setPage(taskPage.getNumber());
        response.setSize(taskPage.getSize());
        response.setTotalElements(taskPage.getTotalElements());
        response.setTotalPages(taskPage.getTotalPages());
        return response;
    }

    /**
     * Get task by ID (must belong to current user).
     */
    @Transactional(readOnly = true)
    public TaskResponse getTask(Integer taskId) {
        User currentUser = farmAccessService.getCurrentUser();
        Task task = taskRepository.findByIdAndUser(taskId, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        return mapToResponse(task);
    }

    /**
     * Update task details.
     */
    @Transactional
    public TaskResponse updateTask(Integer taskId, UpdateTaskRequest request) {
        User currentUser = farmAccessService.getCurrentUser();
        Task task = taskRepository.findByIdAndUser(taskId, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        // Cannot update CANCELLED tasks
        if (task.getStatus() == TaskStatus.CANCELLED) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }

        // Validate dates
        validateTaskDates(
                request.getPlannedDate(),
                request.getDueDate(),
                task.getActualStartDate(),
                task.getActualEndDate());

        // Validate season ownership if changed
        if (request.getSeasonId() != null && !request.getSeasonId().equals(
                task.getSeason() != null ? task.getSeason().getId() : null)) {
            Season newSeason = seasonRepository.findById(request.getSeasonId())
                    .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
            farmAccessService.assertCurrentUserCanAccessSeason(newSeason);
            task.setSeason(newSeason);
        } else if (request.getSeasonId() == null) {
            task.setSeason(null);
        }

        // Update fields
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPlannedDate(request.getPlannedDate());
        task.setDueDate(request.getDueDate());
        task.setNotes(request.getNotes());

        // Recheck overdue status
        if (task.getStatus() == TaskStatus.OVERDUE) {
            // If due date is now in future, revert to IN_PROGRESS or PENDING
            if (task.getDueDate() == null || !task.getDueDate().isBefore(LocalDate.now())) {
                if (task.getActualStartDate() != null) {
                    task.setStatus(TaskStatus.IN_PROGRESS);
                } else {
                    task.setStatus(TaskStatus.PENDING);
                }
            }
        } else if (task.getStatus() != TaskStatus.DONE && task.getStatus() != TaskStatus.CANCELLED) {
            // Check if it should become overdue
            if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())) {
                task.setStatus(TaskStatus.OVERDUE);
            }
        }

        task = taskRepository.save(task);
        log.info("Updated task {}", taskId);

        return mapToResponse(task);
    }

    /**
     * Start a task (set actualStartDate and status to IN_PROGRESS).
     */
    @Transactional
    public TaskResponse startTask(Integer taskId, StartTaskRequest request) {
        User currentUser = farmAccessService.getCurrentUser();
        Task task = taskRepository.findByIdAndUser(taskId, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        // Cannot start DONE or CANCELLED tasks
        if (task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.CANCELLED) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }

        LocalDate startDate = request.getActualStartDate() != null ? request.getActualStartDate() : LocalDate.now();
        task.setActualStartDate(startDate);
        task.setStatus(TaskStatus.IN_PROGRESS);

        task = taskRepository.save(task);
        log.info("Started task {}", taskId);

        return mapToResponse(task);
    }

    /**
     * Mark task as done (set actualEndDate and status to DONE).
     */
    @Transactional
    public TaskResponse doneTask(Integer taskId, TaskDoneRequest request) {
        User currentUser = farmAccessService.getCurrentUser();
        Task task = taskRepository.findByIdAndUser(taskId, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        // Cannot mark CANCELLED tasks as done
        if (task.getStatus() == TaskStatus.CANCELLED) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }

        LocalDate endDate = request.getActualEndDate() != null ? request.getActualEndDate() : LocalDate.now();

        // Validate end date >= start date
        if (task.getActualStartDate() != null && endDate.isBefore(task.getActualStartDate())) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        task.setActualEndDate(endDate);
        task.setStatus(TaskStatus.DONE);

        task = taskRepository.save(task);
        log.info("Marked task {} as done", taskId);

        return mapToResponse(task);
    }

    /**
     * Cancel a task.
     */
    @Transactional
    public TaskResponse cancelTask(Integer taskId) {
        User currentUser = farmAccessService.getCurrentUser();
        Task task = taskRepository.findByIdAndUser(taskId, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        task.setStatus(TaskStatus.CANCELLED);
        task = taskRepository.save(task);
        log.info("Cancelled task {}", taskId);

        return mapToResponse(task);
    }

    /**
     * Delete a task (hard delete, dev only).
     */
    @Transactional
    public void deleteTask(Integer taskId) {
        User currentUser = farmAccessService.getCurrentUser();
        Task task = taskRepository.findByIdAndUser(taskId, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        taskRepository.delete(task);
        log.info("Deleted task {}", taskId);
    }

    /**
     * Get user's seasons for dropdown (minimal data).
     */
    @Transactional(readOnly = true)
    public List<SeasonMinimalResponse> getUserSeasons() {
        List<Integer> accessibleFarmIds = farmAccessService.getAccessibleFarmIdsForCurrentUser();

        return seasonRepository.findAllByPlot_Farm_IdIn(accessibleFarmIds).stream()
                .map(season -> SeasonMinimalResponse.builder()
                        .seasonId(season.getId())
                        .seasonName(season.getSeasonName())
                        .startDate(season.getStartDate())
                        .endDate(season.getEndDate())
                        .plannedHarvestDate(season.getPlannedHarvestDate())
                        .build())
                .toList();
    }

    // ==================== Helper Methods ====================

    /**
     * Auto-update overdue tasks for a specific user.
     */
    private void updateOverdueTasksForUser(User user) {
        List<Task> overdueCandidate = taskRepository.findByUserWithFilters(
                user,
                null,
                null,
                null,
                Pageable.unpaged()).getContent();

        for (Task task : overdueCandidate) {
            if (task.getDueDate() != null &&
                    task.getDueDate().isBefore(LocalDate.now()) &&
                    (task.getStatus() == TaskStatus.PENDING || task.getStatus() == TaskStatus.IN_PROGRESS)) {
                task.setStatus(TaskStatus.OVERDUE);
                taskRepository.save(task);
            }
        }
    }

    /**
     * Validate task dates.
     */
    private void validateTaskDates(LocalDate plannedDate, LocalDate dueDate,
            LocalDate actualStartDate, LocalDate actualEndDate) {
        // Due date must be >= planned date
        if (plannedDate != null && dueDate != null && dueDate.isBefore(plannedDate)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        // Actual end must be >= actual start
        if (actualStartDate != null && actualEndDate != null && actualEndDate.isBefore(actualStartDate)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    /**
     * Map Task entity to TaskResponse DTO.
     */
    private TaskResponse mapToResponse(Task task) {
        return TaskResponse.builder()
                .taskId(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus() != null ? task.getStatus().getCode() : null)
                .plannedDate(task.getPlannedDate())
                .dueDate(task.getDueDate())
                .actualStartDate(task.getActualStartDate())
                .actualEndDate(task.getActualEndDate())
                .notes(task.getNotes())
                .seasonId(task.getSeason() != null ? task.getSeason().getId() : null)
                .seasonName(task.getSeason() != null ? task.getSeason().getSeasonName() : null)
                .userId(task.getUser() != null ? task.getUser().getId() : null)
                .userName(task.getUser() != null ? task.getUser().getUsername() : null)
                .createdAt(task.getCreatedAt())
                .build();
    }
}
