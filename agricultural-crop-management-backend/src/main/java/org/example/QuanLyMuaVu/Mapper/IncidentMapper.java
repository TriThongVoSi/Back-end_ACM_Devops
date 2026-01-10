package org.example.QuanLyMuaVu.Mapper;

import org.example.QuanLyMuaVu.DTO.Response.IncidentResponse;
import org.example.QuanLyMuaVu.Entity.Incident;
import org.springframework.stereotype.Component;

@Component
public class IncidentMapper {

    public IncidentResponse toResponse(Incident incident) {
        if (incident == null) {
            return null;
        }
        return IncidentResponse.builder()
                .incidentId(incident.getId())
                .seasonId(incident.getSeason() != null ? incident.getSeason().getId() : null)
                .seasonName(incident.getSeason() != null ? incident.getSeason().getSeasonName() : null)
                .reportedById(incident.getReportedBy() != null ? Long.valueOf(incident.getReportedBy().getId()) : null)
                .reportedByUsername(incident.getReportedBy() != null ? incident.getReportedBy().getUsername() : null)
                .incidentType(incident.getIncidentType())
                .severity(incident.getSeverity() != null ? incident.getSeverity().name() : null)
                .description(incident.getDescription())
                .status(incident.getStatus() != null ? incident.getStatus().name() : null)
                .deadline(incident.getDeadline())
                .resolvedAt(incident.getResolvedAt())
                .createdAt(incident.getCreatedAt())
                .build();
    }
}
