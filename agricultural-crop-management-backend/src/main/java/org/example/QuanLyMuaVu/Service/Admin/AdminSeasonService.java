package org.example.QuanLyMuaVu.Service.Admin;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.DTO.Request.AdminSeasonUpdateRequest;
import org.example.QuanLyMuaVu.DTO.Response.SeasonDetailResponse;
import org.example.QuanLyMuaVu.DTO.Response.SeasonResponse;
import org.example.QuanLyMuaVu.Entity.Farm;
import org.example.QuanLyMuaVu.Entity.Plot;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Entity.Task;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Mapper.SeasonMapper;
import org.example.QuanLyMuaVu.Repository.SeasonRepository;
import org.example.QuanLyMuaVu.Repository.TaskRepository;
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
 * Admin service for system-wide season management and intervention.
 * Provides read-only listing with filters and admin intervention capabilities
 * for completing seasons with automatic task cancellation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminSeasonService {

    SeasonRepository seasonRepository;
    TaskRepository taskRepository;
    SeasonMapper seasonMapper;

    /**
     * Get all seasons with optional filtering by farmId, cropId, plotId, and
     * status.
     * 
     * @param farmId Filter by farm ID (through plot -> farm relationship)
     * @param status Filter by season status (PLANNED, ACTIVE, COMPLETED, CANCELLED,
     *               ARCHIVED)
     * @param cropId Filter by crop ID
     * @param plotId Filter by plot ID
     * @param page   Page index (0-based)
     * @param size   Page size
     * @return Paginated list of seasons
     */
    public PageResponse<SeasonResponse> getAllSeasons(
            Integer farmId,
            String status,
            Integer cropId,
            Integer plotId,
            int page,
            int size) {
        log.info("Admin fetching all seasons - farmId: {}, status: {}, cropId: {}, plotId: {}, page: {}, size: {}",
                farmId, status, cropId, plotId, page, size);

        Pageable pageable = PageRequest.of(page, size);

        Specification<Season> spec = buildSeasonSpecification(farmId, status, cropId, plotId);
        Page<Season> seasonPage = seasonRepository.findAll(spec, pageable);

        List<SeasonResponse> content = seasonPage.getContent().stream()
                .map(seasonMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(seasonPage, content);
    }

    /**
     * Get season detail by ID.
     * 
     * @param seasonId The season ID
     * @return Detailed season response including related entities
     */
    public SeasonDetailResponse getSeasonById(Integer seasonId) {
        log.info("Admin fetching season detail for ID: {}", seasonId);

        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));

        return seasonMapper.toDetailResponse(season);
    }

    /**
     * Count pending (non-DONE) tasks for a season.
     * Used for UX warning when completing a season.
     * 
     * @param seasonId The season ID
     * @return Count of pending tasks
     */
    public Long getPendingTaskCount(Integer seasonId) {
        return taskRepository.countBySeason_IdAndStatusNot(seasonId, TaskStatus.DONE);
    }

    /**
     * Update a season (admin intervention).
     * If status is changed to COMPLETED:
     * - Validates that endDate and actualYieldKg are provided
     * - Auto-cancels all pending tasks with log message
     * 
     * @param seasonId The season ID to update
     * @param request  The update request containing new values
     * @return Updated season response
     */
    @Transactional
    public SeasonResponse updateSeason(Integer seasonId, AdminSeasonUpdateRequest request) {
        log.info("Admin updating season {} with request: {}", seasonId, request);

        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));

        boolean isCompletingNow = false;

        // Handle status update
        if (request.getStatus() != null) {
            SeasonStatus newStatus = SeasonStatus.fromCode(request.getStatus());

            // Check if transitioning to COMPLETED
            if (newStatus == SeasonStatus.COMPLETED && season.getStatus() != SeasonStatus.COMPLETED) {
                isCompletingNow = true;

                // Business Rule: Validate endDate and actualYieldKg
                if (request.getEndDate() == null || request.getActualYieldKg() == null) {
                    throw new AppException(ErrorCode.SEASON_COMPLETION_REQUIRES_YIELD_AND_DATE);
                }
            }

            season.setStatus(newStatus);
        }

        // Update endDate if provided
        if (request.getEndDate() != null) {
            season.setEndDate(request.getEndDate());
        }

        // Update actualYieldKg if provided
        if (request.getActualYieldKg() != null) {
            season.setActualYieldKg(request.getActualYieldKg());
        }

        // Update notes if provided
        if (request.getNotes() != null) {
            season.setNotes(request.getNotes());
        }

        // Business Rule: Auto-cancel pending tasks when completing a season
        if (isCompletingNow) {
            List<Task> pendingTasks = taskRepository.findBySeason_IdAndStatusNot(seasonId, TaskStatus.DONE);

            for (Task task : pendingTasks) {
                task.setStatus(TaskStatus.CANCELLED);
                // Append cancellation note
                String existingNotes = task.getNotes() != null ? task.getNotes() + "\n" : "";
                task.setNotes(existingNotes + "Auto-cancelled by Season Completion (Admin Intervention)");
                taskRepository.save(task);
            }

            if (!pendingTasks.isEmpty()) {
                log.info("Admin Intervention: Auto-cancelled {} pending tasks for season {}",
                        pendingTasks.size(), seasonId);
            }
        }

        Season savedSeason = seasonRepository.save(season);
        return seasonMapper.toResponse(savedSeason);
    }

    /**
     * Build JPA Specification for filtering seasons.
     */
    private Specification<Season> buildSeasonSpecification(
            Integer farmId,
            String status,
            Integer cropId,
            Integer plotId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by farmId (through plot -> farm)
            if (farmId != null) {
                Join<Season, Plot> plotJoin = root.join("plot", JoinType.LEFT);
                Join<Plot, Farm> farmJoin = plotJoin.join("farm", JoinType.LEFT);
                predicates.add(cb.equal(farmJoin.get("id"), farmId));
            }

            // Filter by status
            if (status != null && !status.isEmpty()) {
                try {
                    SeasonStatus seasonStatus = SeasonStatus.fromCode(status);
                    predicates.add(cb.equal(root.get("status"), seasonStatus));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid season status filter: {}", status);
                }
            }

            // Filter by cropId
            if (cropId != null) {
                predicates.add(cb.equal(root.get("crop").get("id"), cropId));
            }

            // Filter by plotId
            if (plotId != null) {
                predicates.add(cb.equal(root.get("plot").get("id"), plotId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
