package org.example.QuanLyMuaVu.Pattern.Observer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Observer Pattern: Domain Event Listener.
 * <p>
 * Listens to domain events and handles cross-cutting concerns like:
 * - Logging
 * - Notifications
 * - Statistics updates
 * - Audit trail persistence
 * <p>
 * Uses Spring's @EventListener for automatic event subscription.
 * 
 * @Async enables non-blocking event processing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventListener {

    // TODO: Inject notification service when available
    // private final NotificationService notificationService;

    @EventListener
    @Async
    public void handleSeasonCreated(SeasonCreatedEvent event) {
        log.info("[EVENT] Season created: id={}, name={}, plotId={}, cropId={}",
                event.getSeasonId(),
                event.getSeasonName(),
                event.getPlotId(),
                event.getCropId());

        // TODO: Future enhancements:
        // 1. Auto-generate tasks from crop-specific templates
        // 2. Send notification to farm manager
        // 3. Update farm dashboard statistics
    }

    @EventListener
    @Async
    public void handleTaskCompleted(TaskCompletedEvent event) {
        log.info("[EVENT] Task completed: id={}, title={}, seasonId={}, previousStatus={}",
                event.getTaskId(),
                event.getTaskTitle(),
                event.getSeasonId(),
                event.getPreviousStatus());

        // TODO: Future enhancements:
        // 1. Update season progress percentage
        // 2. Trigger follow-up tasks if defined
        // 3. Send completion notification
    }

    @EventListener
    @Async
    public void handleIncidentReported(IncidentReportedEvent event) {
        log.info("[EVENT] Incident reported: id={}, type={}, severity={}, seasonId={}, by user={}",
                event.getIncidentId(),
                event.getIncidentType(),
                event.getSeverity(),
                event.getSeasonId(),
                event.getReportedByUserId());

        // HIGH severity incidents should trigger auto-task creation
        if ("HIGH".equalsIgnoreCase(event.getSeverity())) {
            log.warn("[EVENT] HIGH severity incident detected - consider auto-creating mitigation task");
            // TODO: Auto-create mitigation task:
            // taskService.createMitigationTask(event.getSeasonId(), event.getIncidentId());
        }
    }
}
