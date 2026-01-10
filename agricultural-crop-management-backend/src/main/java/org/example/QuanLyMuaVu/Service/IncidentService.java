package org.example.QuanLyMuaVu.Service;

import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.DTO.Request.CreateIncidentRequest;
import org.example.QuanLyMuaVu.DTO.Request.IncidentStatusUpdateRequest;
import org.example.QuanLyMuaVu.DTO.Request.UpdateIncidentRequest;
import org.example.QuanLyMuaVu.DTO.Response.IncidentResponse;
import org.example.QuanLyMuaVu.Entity.Incident;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Enums.IncidentSeverity;
import org.example.QuanLyMuaVu.Enums.IncidentStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.IncidentRepository;
import org.example.QuanLyMuaVu.Repository.SeasonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class IncidentService {

    IncidentRepository incidentRepository;
    SeasonRepository seasonRepository;
    FarmAccessService farmAccessService;

    /**
     * List incidents with pagination and filters
     */
    public PageResponse<IncidentResponse> listIncidents(
            Integer seasonId,
            String status,
            String severity,
            String type,
            String q,
            LocalDate from,
            LocalDate to,
            int page,
            int size,
            String sort) {
        // Validate seasonId and ownership
        Season season = getSeasonForCurrentFarmer(seasonId);

        // Build dynamic specification
        Specification<Incident> spec = buildSpecification(season, status, severity, type, q, from, to);

        // Determine sort direction
        Sort sortOrder = Sort.by(Sort.Direction.DESC, "createdAt");
        if (StringUtils.hasText(sort)) {
            if (sort.startsWith("-")) {
                sortOrder = Sort.by(Sort.Direction.DESC, sort.substring(1));
            } else {
                sortOrder = Sort.by(Sort.Direction.ASC, sort);
            }
        }

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<Incident> incidentPage = incidentRepository.findAll(spec, pageable);

        List<IncidentResponse> items = incidentPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.of(incidentPage, items);
    }

    /**
     * List incidents by season (legacy support, returns list)
     */
    public List<IncidentResponse> listBySeason(Integer seasonId) {
        Season season = getSeasonForCurrentFarmer(seasonId);
        return incidentRepository.findAllBySeason(season)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get incident by ID
     */
    @Transactional(readOnly = true)
    public IncidentResponse getById(Integer id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        // Verify ownership
        getSeasonForCurrentFarmer(incident.getSeason().getId());
        return toResponse(incident);
    }

    /**
     * Create a new incident
     */
    public IncidentResponse create(Integer seasonId, CreateIncidentRequest request) {
        Season season = getSeasonForCurrentFarmer(seasonId);
        User reporter = getCurrentUser();

        // Validate deadline if provided
        if (request.getDeadline() != null && request.getDeadline().isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.INVALID_DEADLINE);
        }

        Incident incident = Incident.builder()
                .season(season)
                .reportedBy(reporter)
                .incidentType(request.getIncidentType())
                .severity(IncidentSeverity.fromCode(request.getSeverity()))
                .description(request.getDescription())
                .status(IncidentStatus.OPEN)
                .deadline(request.getDeadline())
                .createdAt(LocalDateTime.now())
                .build();

        Incident saved = incidentRepository.save(incident);
        return toResponse(saved);
    }

    /**
     * Update incident details (not status)
     */
    public IncidentResponse update(Integer id, UpdateIncidentRequest request) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        // Ensure the current farmer is a member of the season's farm
        getSeasonForCurrentFarmer(incident.getSeason().getId());

        if (request.getIncidentType() != null) {
            incident.setIncidentType(request.getIncidentType());
        }
        if (request.getSeverity() != null) {
            incident.setSeverity(IncidentSeverity.fromCode(request.getSeverity()));
        }
        if (request.getDescription() != null) {
            incident.setDescription(request.getDescription());
        }
        if (request.getDeadline() != null) {
            if (request.getDeadline().isBefore(LocalDate.now())) {
                throw new AppException(ErrorCode.INVALID_DEADLINE);
            }
            incident.setDeadline(request.getDeadline());
        }

        Incident saved = incidentRepository.save(incident);
        return toResponse(saved);
    }

    /**
     * Update incident status (PATCH endpoint)
     */
    public IncidentResponse updateStatus(Integer id, IncidentStatusUpdateRequest request) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        // Verify ownership
        getSeasonForCurrentFarmer(incident.getSeason().getId());

        IncidentStatus newStatus = IncidentStatus.fromCode(request.getStatus());
        IncidentStatus currentStatus = incident.getStatus();

        // Validate status transition
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new AppException(ErrorCode.INVALID_INCIDENT_STATUS_TRANSITION);
        }

        // If resolving, require resolution note and append to description
        if (newStatus == IncidentStatus.RESOLVED) {
            if (!StringUtils.hasText(request.getResolutionNote())) {
                throw new AppException(ErrorCode.RESOLUTION_NOTE_REQUIRED);
            }

            // Append resolution note to description
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String updatedDescription = incident.getDescription() +
                    "\n\n[Resolution @" + timestamp + "]\n" + request.getResolutionNote();
            incident.setDescription(updatedDescription);
            incident.setResolvedAt(LocalDateTime.now());
        }

        incident.setStatus(newStatus);
        Incident saved = incidentRepository.save(incident);
        return toResponse(saved);
    }

    /**
     * Delete incident
     */
    public void delete(Integer id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        // Ownership / membership check
        getSeasonForCurrentFarmer(incident.getSeason().getId());

        // Optional: prevent deletion of resolved incidents
        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            throw new AppException(ErrorCode.CANNOT_DELETE_RESOLVED_INCIDENT);
        }

        incidentRepository.delete(incident);
    }

    /**
     * Get incident summary counts for a season
     */
    @Transactional(readOnly = true)
    public IncidentSummary getSummary(Integer seasonId) {
        Season season = getSeasonForCurrentFarmer(seasonId);

        long openCount = incidentRepository.countBySeasonAndStatus(season, IncidentStatus.OPEN);
        long inProgressCount = incidentRepository.countBySeasonAndStatus(season, IncidentStatus.IN_PROGRESS);
        long resolvedCount = incidentRepository.countBySeasonAndStatus(season, IncidentStatus.RESOLVED);
        long cancelledCount = incidentRepository.countBySeasonAndStatus(season, IncidentStatus.CANCELLED);

        return new IncidentSummary(openCount, inProgressCount, resolvedCount, cancelledCount);
    }

    // ============ Helper Methods ============

    private Specification<Incident> buildSpecification(
            Season season,
            String status,
            String severity,
            String type,
            String q,
            LocalDate from,
            LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by season
            predicates.add(cb.equal(root.get("season"), season));

            // Status filter
            if (StringUtils.hasText(status)) {
                IncidentStatus statusEnum = IncidentStatus.fromCode(status);
                if (statusEnum != null) {
                    predicates.add(cb.equal(root.get("status"), statusEnum));
                }
            }

            // Severity filter
            if (StringUtils.hasText(severity)) {
                IncidentSeverity severityEnum = IncidentSeverity.fromCode(severity);
                if (severityEnum != null) {
                    predicates.add(cb.equal(root.get("severity"), severityEnum));
                }
            }

            // Type filter
            if (StringUtils.hasText(type)) {
                predicates.add(cb.equal(root.get("incidentType"), type));
            }

            // Search in description (min 2 chars)
            if (StringUtils.hasText(q) && q.length() >= 2) {
                predicates.add(cb.like(cb.lower(root.get("description")), "%" + q.toLowerCase() + "%"));
            }

            // Date range filter on createdAt
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Season getSeasonForCurrentFarmer(Integer seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        farmAccessService.assertCurrentUserCanAccessSeason(season);
        return season;
    }

    private User getCurrentUser() {
        return farmAccessService.getCurrentUser();
    }

    private IncidentResponse toResponse(Incident incident) {
        Season season = incident.getSeason();
        User reporter = incident.getReportedBy();
        return IncidentResponse.builder()
                .incidentId(incident.getId())
                .seasonId(season != null ? season.getId() : null)
                .seasonName(season != null ? season.getSeasonName() : null)
                .reportedById(reporter != null ? reporter.getId() : null)
                .reportedByUsername(reporter != null ? reporter.getUsername() : null)
                .incidentType(incident.getIncidentType())
                .severity(incident.getSeverity() != null ? incident.getSeverity().name() : null)
                .description(incident.getDescription())
                .status(incident.getStatus() != null ? incident.getStatus().name() : null)
                .deadline(incident.getDeadline())
                .resolvedAt(incident.getResolvedAt())
                .createdAt(incident.getCreatedAt())
                .build();
    }

    private boolean isValidStatusTransition(IncidentStatus currentStatus, IncidentStatus targetStatus) {
        if (targetStatus == null) {
            return false;
        }
        if (currentStatus == null) {
            return targetStatus == IncidentStatus.OPEN;
        }
        if (currentStatus == targetStatus) {
            return true;
        }
        return switch (currentStatus) {
            case OPEN -> targetStatus == IncidentStatus.IN_PROGRESS
                    || targetStatus == IncidentStatus.RESOLVED
                    || targetStatus == IncidentStatus.CANCELLED;
            case IN_PROGRESS -> targetStatus == IncidentStatus.RESOLVED
                    || targetStatus == IncidentStatus.CANCELLED;
            case RESOLVED, CANCELLED -> false;
        };
    }

    // ============ Inner class for summary ============
    public record IncidentSummary(long openCount, long inProgressCount, long resolvedCount, long cancelledCount) {
    }
}
