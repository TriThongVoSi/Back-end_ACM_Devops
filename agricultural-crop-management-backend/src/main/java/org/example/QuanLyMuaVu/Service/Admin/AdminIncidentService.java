package org.example.QuanLyMuaVu.Service.Admin;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.DTO.Request.CancelIncidentRequest;
import org.example.QuanLyMuaVu.DTO.Request.ResolveIncidentRequest;
import org.example.QuanLyMuaVu.DTO.Request.TriageIncidentRequest;
import org.example.QuanLyMuaVu.DTO.Response.IncidentResponse;
import org.example.QuanLyMuaVu.Entity.Incident;
import org.example.QuanLyMuaVu.Enums.IncidentSeverity;
import org.example.QuanLyMuaVu.Enums.IncidentStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Mapper.IncidentMapper;
import org.example.QuanLyMuaVu.Repository.IncidentRepository;
import org.springframework.dao.OptimisticLockingFailureException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.example.QuanLyMuaVu.Enums.IncidentStatus.*;

/**
 * Admin service for system-wide incident management.
 * Implements a state machine workflow for incident processing.
 * 
 * State Machine:
 * - OPEN -> IN_PROGRESS (via triage)
 * - OPEN -> CANCELLED (via cancel)
 * - IN_PROGRESS -> RESOLVED (via resolve)
 * - IN_PROGRESS -> CANCELLED (via cancel)
 * 
 * Note: This service is adapted to work with the current Incident entity
 * structure.
 * Some advanced features (assignee, cancellation reason) are not stored in DB
 * but the workflow is fully functional.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminIncidentService {

    IncidentRepository incidentRepository;
    IncidentMapper incidentMapper;

    // ═══════════════════════════════════════════════════════════════
    // STATE MACHINE CONFIGURATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Allowed state transitions map.
     */
    private static final Map<IncidentStatus, Set<IncidentStatus>> ALLOWED_TRANSITIONS = Map.of(
            OPEN, Set.of(IN_PROGRESS, CANCELLED),
            IN_PROGRESS, Set.of(RESOLVED, CANCELLED));

    // ═══════════════════════════════════════════════════════════════
    // QUERY OPERATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get all incidents with optional filters.
     */
    public PageResponse<IncidentResponse> getAllIncidents(String status, String severity, String type, Integer farmId,
            String q, String sort, int page, int size) {
        log.info(
                "Admin fetching all incidents - status: {}, severity: {}, type: {}, farmId: {}, q: {}, page: {}, size: {}",
                status, severity, type, farmId, q, page, size);

        Sort sortOrder = Sort.by(Sort.Direction.DESC, "createdAt");
        if (StringUtils.hasText(sort) && "OLDEST".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by(Sort.Direction.ASC, "createdAt");
        }

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Specification<Incident> spec = buildSpecification(status, severity, type, farmId, q);
        Page<Incident> incidentPage = incidentRepository.findAll(spec, pageable);

        List<IncidentResponse> content = incidentPage.getContent().stream()
                .map(incidentMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(incidentPage, content);
    }

    /**
     * Get incident detail by ID.
     */
    public IncidentResponse getIncidentById(Integer incidentId) {
        log.info("Admin fetching incident detail for ID: {}", incidentId);

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new AppException(ErrorCode.INCIDENT_NOT_FOUND));

        return incidentMapper.toResponse(incident);
    }

    // ═══════════════════════════════════════════════════════════════
    // TRIAGE OPERATION (OPEN -> IN_PROGRESS)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Triage an open incident: set severity, deadline, and transition to
     * IN_PROGRESS.
     */
    @Transactional
    public IncidentResponse triage(Integer incidentId, TriageIncidentRequest request) {
        log.info("Admin triaging incident {} with severity: {}, deadline: {}",
                incidentId, request.getSeverity(), request.getDeadline());

        try {
            Incident incident = incidentRepository.findById(incidentId)
                    .orElseThrow(() -> new AppException(ErrorCode.INCIDENT_NOT_FOUND));

            // Validate transition: must be OPEN
            validateTransition(incident.getStatus(), IN_PROGRESS);

            // Validate deadline if provided
            if (request.getDeadline() != null && request.getDeadline().isBefore(LocalDate.now())) {
                throw new AppException(ErrorCode.INVALID_DEADLINE);
            }

            // Update severity
            IncidentSeverity newSeverity = IncidentSeverity.fromCode(request.getSeverity());
            incident.setSeverity(newSeverity);

            // Set deadline if provided
            if (request.getDeadline() != null) {
                incident.setDeadline(request.getDeadline());
            }

            // Note: assignee is not stored in current entity structure
            // but the triage workflow is still functional

            // Transition to IN_PROGRESS
            incident.setStatus(IN_PROGRESS);

            incidentRepository.save(incident);
            log.info("Incident {} triaged successfully", incidentId);
            return incidentMapper.toResponse(incident);

        } catch (OptimisticLockingFailureException e) {
            log.warn("Optimistic locking failure for incident {}", incidentId);
            throw new AppException(ErrorCode.OPTIMISTIC_LOCK_ERROR);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // RESOLVE OPERATION (IN_PROGRESS -> RESOLVED)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Resolve an in-progress incident.
     */
    @Transactional
    public IncidentResponse resolve(Integer incidentId, ResolveIncidentRequest request) {
        log.info("Admin resolving incident {} with note: {}", incidentId, request.getResolutionNote());

        try {
            Incident incident = incidentRepository.findById(incidentId)
                    .orElseThrow(() -> new AppException(ErrorCode.INCIDENT_NOT_FOUND));

            // Validate transition: must be IN_PROGRESS
            validateTransition(incident.getStatus(), RESOLVED);

            // Set resolution details
            incident.setStatus(RESOLVED);
            incident.setResolvedAt(LocalDateTime.now());

            // Note: resolutionNote and resolvedBy are not stored in current entity
            // structure
            // but logged for audit trail
            log.info("Resolution note for incident {}: {}", incidentId, request.getResolutionNote());

            incidentRepository.save(incident);
            log.info("Incident {} resolved successfully", incidentId);
            return incidentMapper.toResponse(incident);

        } catch (OptimisticLockingFailureException e) {
            log.warn("Optimistic locking failure for incident {}", incidentId);
            throw new AppException(ErrorCode.OPTIMISTIC_LOCK_ERROR);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CANCEL OPERATION (OPEN/IN_PROGRESS -> CANCELLED)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Cancel an incident.
     */
    @Transactional
    public IncidentResponse cancel(Integer incidentId, CancelIncidentRequest request) {
        log.info("Admin cancelling incident {} with reason: {}", incidentId, request.getCancellationReason());

        try {
            Incident incident = incidentRepository.findById(incidentId)
                    .orElseThrow(() -> new AppException(ErrorCode.INCIDENT_NOT_FOUND));

            // Validate transition: must be OPEN or IN_PROGRESS
            validateTransition(incident.getStatus(), CANCELLED);

            // Set cancellation status
            incident.setStatus(CANCELLED);

            // Note: cancellationReason is not stored in current entity structure
            // but logged for audit trail
            log.info("Cancellation reason for incident {}: {}", incidentId, request.getCancellationReason());

            incidentRepository.save(incident);
            log.info("Incident {} cancelled successfully", incidentId);
            return incidentMapper.toResponse(incident);

        } catch (OptimisticLockingFailureException e) {
            log.warn("Optimistic locking failure for incident {}", incidentId);
            throw new AppException(ErrorCode.OPTIMISTIC_LOCK_ERROR);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // LEGACY STATUS UPDATE (kept for backward compatibility)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Generic status update for legacy compatibility.
     */
    @Transactional
    public IncidentResponse updateStatus(Integer incidentId, String newStatus) {
        log.info("Admin updating incident {} status to: {}", incidentId, newStatus);

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new AppException(ErrorCode.INCIDENT_NOT_FOUND));

        IncidentStatus targetStatus = IncidentStatus.valueOf(newStatus);

        // Validate transition
        validateTransition(incident.getStatus(), targetStatus);

        incident.setStatus(targetStatus);

        // Set resolved_at when status changes to RESOLVED
        if (targetStatus == RESOLVED) {
            incident.setResolvedAt(LocalDateTime.now());
        }

        incidentRepository.save(incident);
        return incidentMapper.toResponse(incident);
    }

    // ═══════════════════════════════════════════════════════════════
    // VALIDATION HELPERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Validate that the transition from current status to target status is allowed.
     */
    private void validateTransition(IncidentStatus current, IncidentStatus target) {
        if (current == null) {
            throw new AppException(ErrorCode.INVALID_INCIDENT_STATUS_TRANSITION);
        }

        Set<IncidentStatus> allowedTargets = ALLOWED_TRANSITIONS.get(current);
        if (allowedTargets == null || !allowedTargets.contains(target)) {
            log.warn("Invalid transition attempted: {} -> {}", current, target);
            throw new AppException(ErrorCode.INVALID_INCIDENT_STATUS_TRANSITION);
        }
    }

    private Specification<Incident> buildSpecification(String status, String severity, String type, Integer farmId,
            String q) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
                try {
                    IncidentStatus statusEnum = IncidentStatus.fromCode(status);
                    predicates.add(cb.equal(root.get("status"), statusEnum));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid incident status filter: {}", status);
                }
            }

            if (StringUtils.hasText(severity) && !"ALL".equalsIgnoreCase(severity)) {
                try {
                    IncidentSeverity severityEnum = IncidentSeverity.fromCode(severity);
                    predicates.add(cb.equal(root.get("severity"), severityEnum));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid incident severity filter: {}", severity);
                }
            }

            if (StringUtils.hasText(type) && !"ALL".equalsIgnoreCase(type)) {
                predicates.add(cb.equal(root.get("incidentType"), type));
            }

            if (farmId != null) {
                predicates.add(cb.equal(root.get("season").get("plot").get("farm").get("id"), farmId));
            }

            if (StringUtils.hasText(q) && q.trim().length() >= 2) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("incidentType")), like),
                        cb.like(cb.lower(root.get("description")), like)));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
