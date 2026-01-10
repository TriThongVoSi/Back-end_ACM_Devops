package org.example.QuanLyMuaVu.Pattern.Observer;

import lombok.Getter;
import org.example.QuanLyMuaVu.Entity.Incident;

/**
 * Observer Pattern: Incident Reported Event.
 * <p>
 * Published when a new incident (pest outbreak, disease, weather damage) is
 * reported.
 * Listeners can use this to:
 * - Auto-create mitigation tasks for HIGH severity incidents
 * - Send alerts to farm managers
 * - Trigger AI assistant to suggest remediation
 */
@Getter
public class IncidentReportedEvent extends DomainEvent {

    private final Integer incidentId;
    private final String incidentType;
    private final String severity;
    private final Integer seasonId;
    private final Long reportedByUserId;

    public IncidentReportedEvent(Incident incident) {
        super("Incident", incident.getId() != null ? incident.getId().toString() : "unknown");
        this.incidentId = incident.getId();
        this.incidentType = incident.getIncidentType();
        this.severity = incident.getSeverity() != null ? incident.getSeverity().name() : null;
        this.seasonId = incident.getSeason() != null ? incident.getSeason().getId() : null;
        this.reportedByUserId = incident.getReportedBy() != null ? incident.getReportedBy().getId() : null;
    }

    @Override
    public String getEventType() {
        return "INCIDENT_REPORTED";
    }
}
