package org.example.QuanLyMuaVu.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.Entity.AuditLog;
import org.example.QuanLyMuaVu.Entity.Farm;
import org.example.QuanLyMuaVu.Repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for creating audit logs of critical operations.
 * 
 * Transaction Strategy:
 * - Non-critical operations (soft delete, restore, create, update): Use
 * REQUIRES_NEW propagation.
 * Audit logs persist independently, even if main operation fails.
 * Audit failures are logged but don't fail the business operation.
 * 
 * - Critical operations (hard delete): Use MANDATORY propagation.
 * Audit logging runs in the same transaction as the delete.
 * If audit fails, the entire transaction (including delete) is rolled back.
 * This ensures we NEVER have a permanent deletion without an audit trail.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditLogService {

    AuditLogRepository auditLogRepository;
    ObjectMapper objectMapper;

    /**
     * Log non-critical farm operations (CREATE, UPDATE, SOFT_DELETE, RESTORE).
     * 
     * Uses REQUIRES_NEW propagation to ensure audit log is saved in a separate
     * transaction.
     * If the main operation fails, the audit log still persists.
     * If audit logging fails, it's logged as an error but doesn't fail the business
     * operation.
     * 
     * Rationale: For recoverable operations, audit failure shouldn't block users.
     * The business can recover from these operations even without audit trails.
     *
     * @param farm        The farm entity being operated on
     * @param operation   The operation type (CREATE, UPDATE, SOFT_DELETE, RESTORE)
     * @param performedBy Username of the user performing the operation
     * @param reason      Optional reason provided by the user
     * @param ipAddress   IP address of the request
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFarmOperation(
            Farm farm,
            String operation,
            String performedBy,
            String reason,
            String ipAddress) {
        try {
            createAuditLog(farm, operation, performedBy, reason, ipAddress);
            log.info(
                    "[AUDIT] Non-critical operation logged: entityType=FARM, entityId={}, operation={}, performedBy={}",
                    farm.getId(), operation, performedBy);

        } catch (Exception e) {
            // Log error but don't fail the operation
            // Audit logging failure should not prevent non-critical business operations
            log.error(
                    "[AUDIT_FAILURE] Failed to create audit log for farm operation: farmId={}, operation={}, error={}",
                    farm.getId(), operation, e.getMessage(), e);

            // TODO: Send alert to monitoring system (e.g., DataDog, CloudWatch, PagerDuty)
            // This is a degraded state that should be investigated
        }
    }

    /**
     * Log CRITICAL farm operations (HARD_DELETE) that require mandatory audit
     * trails.
     * 
     * Uses MANDATORY propagation - this method MUST be called within an existing
     * transaction.
     * The audit log is saved in the SAME transaction as the delete operation.
     * 
     * If audit logging fails, an exception is thrown, causing the entire
     * transaction
     * (including the delete) to be rolled back.
     * 
     * Rationale: Hard delete is irreversible. We MUST have an audit trail, or the
     * operation
     * should not proceed. This ensures compliance and data governance requirements.
     *
     * @param farm        The farm entity being operated on (BEFORE deletion)
     * @param operation   The operation type (should be HARD_DELETE)
     * @param performedBy Username of the user performing the operation
     * @param reason      MANDATORY reason for hard delete (enforced by caller)
     * @param ipAddress   IP address of the request
     * @throws RuntimeException if audit logging fails, causing transaction rollback
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void logFarmOperationCritical(
            Farm farm,
            String operation,
            String performedBy,
            String reason,
            String ipAddress) {
        try {
            createAuditLog(farm, operation, performedBy, reason, ipAddress);
            log.info(
                    "[AUDIT_CRITICAL] Critical operation logged: entityType=FARM, entityId={}, operation={}, performedBy={}",
                    farm.getId(), operation, performedBy);

        } catch (Exception e) {
            // For critical operations, we MUST fail if audit logging fails
            log.error("[AUDIT_CRITICAL_FAILURE] CRITICAL: Failed to create audit log for HARD_DELETE. " +
                    "Transaction will be rolled back. farmId={}, operation={}, error={}",
                    farm.getId(), operation, e.getMessage(), e);

            // TODO: Send CRITICAL alert to monitoring system
            // This should trigger immediate investigation

            // Re-throw to cause transaction rollback
            throw new RuntimeException("Critical audit logging failed for " + operation +
                    " on Farm ID " + farm.getId() + ". Operation aborted for data governance.", e);
        }
    }

    /**
     * Shared logic to create audit log entry.
     * Minimizes PII in snapshot by redacting sensitive fields if needed.
     */
    private void createAuditLog(
            Farm farm,
            String operation,
            String performedBy,
            String reason,
            String ipAddress) throws JsonProcessingException {

        // Serialize farm to JSON snapshot
        // TODO: Consider redacting sensitive PII fields (e.g., detailed address) for
        // GDPR compliance
        String snapshot = objectMapper.writeValueAsString(farm);

        AuditLog auditLog = AuditLog.builder()
                .entityType("FARM")
                .entityId(farm.getId())
                .operation(operation)
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .snapshotDataJson(snapshot)
                .reason(reason)
                .ipAddress(ipAddress)
                .build();

        auditLogRepository.save(auditLog);
    }

    /**
     * Retrieve audit trail for a specific farm.
     */
    public java.util.List<AuditLog> getFarmAuditTrail(Integer farmId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByPerformedAtDesc("FARM", farmId);
    }
}
