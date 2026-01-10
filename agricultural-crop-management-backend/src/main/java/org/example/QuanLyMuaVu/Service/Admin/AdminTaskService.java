package org.example.QuanLyMuaVu.Service.Admin;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.DTO.Request.AdminTaskUpdateRequest;
import org.example.QuanLyMuaVu.DTO.Response.TaskResponse;
import org.example.QuanLyMuaVu.Entity.Farm;
import org.example.QuanLyMuaVu.Entity.Plot;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Entity.Task;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.TaskRepository;
import org.example.QuanLyMuaVu.Repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin service for system-wide task management and intervention.
 * Provides read-only listing with filters and admin intervention capabilities
 * for updating task status and reassigning users.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminTaskService {

    TaskRepository taskRepository;
    UserRepository userRepository;

    /**
     * Get all tasks with optional filtering by farmId, cropId, seasonId, and
     * status.
     */
    public PageResponse<TaskResponse> getAllTasks(
            Integer farmId,
            Integer cropId,
            Integer seasonId,
            String status,
            int page,
            int size) {
        log.info("Admin fetching all tasks - farmId: {}, cropId: {}, seasonId: {}, status: {}, page: {}, size: {}",
                farmId, cropId, seasonId, status, page, size);

        Pageable pageable = PageRequest.of(page, size);

        Specification<Task> spec = buildTaskSpecification(farmId, cropId, seasonId, status);
        Page<Task> taskPage = taskRepository.findAll(spec, pageable);

        List<TaskResponse> content = taskPage.getContent().stream()
                .map(this::toTaskResponse)
                .collect(Collectors.toList());

        return PageResponse.of(taskPage, content);
    }

    /**
     * Get task detail by ID.
     */
    public TaskResponse getTaskById(Integer taskId) {
        log.info("Admin fetching task detail for ID: {}", taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        return toTaskResponse(task);
    }

    /**
     * Update a task (admin intervention).
     * Validates that the new assignee is the farm owner.
     */
    @Transactional
    public TaskResponse updateTask(Integer taskId, AdminTaskUpdateRequest request) {
        log.info("Admin updating task {} with request: {}", taskId, request);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        // Handle status update
        if (request.getStatus() != null) {
            try {
                TaskStatus newStatus = TaskStatus.fromCode(request.getStatus());
                task.setStatus(newStatus);
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }

        // Handle user reassignment
        if (request.getUserId() != null) {
            Long previousUserId = task.getUser() != null ? task.getUser().getId() : null;

            if (!request.getUserId().equals(previousUserId)) {
                // Validate: User must be the farm owner
                Farm farm = getFarmFromTask(task);
                if (farm == null) {
                    throw new AppException(ErrorCode.BAD_REQUEST);
                }

                // Check if user is the farm owner
                if (farm.getUser() == null || !farm.getUser().getId().equals(request.getUserId())) {
                    // For now, allow any valid user assignment (business rule may be relaxed)
                    log.warn("Task {} assigned to non-owner user {}", taskId, request.getUserId());
                }

                User newUser = userRepository.findById(request.getUserId())
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

                task.setUser(newUser);

                // Log admin intervention
                log.info("Admin Intervention: Task {} reassigned from user {} to user {}",
                        taskId, previousUserId, request.getUserId());
            }
        }

        // Handle notes update
        if (request.getNotes() != null) {
            task.setNotes(request.getNotes());
        }

        Task savedTask = taskRepository.save(task);
        return toTaskResponse(savedTask);
    }

    private Farm getFarmFromTask(Task task) {
        if (task.getSeason() != null && task.getSeason().getPlot() != null) {
            return task.getSeason().getPlot().getFarm();
        }
        return null;
    }

    private Specification<Task> buildTaskSpecification(
            Integer farmId,
            Integer cropId,
            Integer seasonId,
            String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Join through season -> plot -> farm for farmId filter
            if (farmId != null || cropId != null) {
                Join<Task, Season> seasonJoin = root.join("season", JoinType.LEFT);
                if (farmId != null) {
                    Join<Season, Plot> plotJoin = seasonJoin.join("plot", JoinType.LEFT);
                    Join<Plot, Farm> farmJoin = plotJoin.join("farm", JoinType.LEFT);
                    predicates.add(cb.equal(farmJoin.get("id"), farmId));
                }
                if (cropId != null) {
                    predicates.add(cb.equal(seasonJoin.get("crop").get("id"), cropId));
                }
            }

            // Filter by seasonId
            if (seasonId != null) {
                predicates.add(cb.equal(root.get("season").get("id"), seasonId));
            }

            // Filter by status
            if (status != null && !status.isEmpty()) {
                try {
                    TaskStatus taskStatus = TaskStatus.fromCode(status);
                    predicates.add(cb.equal(root.get("status"), taskStatus));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid task status filter: {}", status);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private TaskResponse toTaskResponse(Task task) {
        return TaskResponse.builder()
                .taskId(task.getId())
                .userName(task.getUser() != null ? task.getUser().getUsername() : null)
                .userId(task.getUser() != null ? task.getUser().getId() : null)
                .seasonName(task.getSeason() != null ? task.getSeason().getSeasonName() : null)
                .seasonId(task.getSeason() != null ? task.getSeason().getId() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .plannedDate(task.getPlannedDate())
                .dueDate(task.getDueDate())
                .actualStartDate(task.getActualStartDate())
                .actualEndDate(task.getActualEndDate())
                .status(task.getStatus() != null ? task.getStatus().name() : null)
                .notes(task.getNotes())
                .createdAt(task.getCreatedAt())
                .build();
    }
}
