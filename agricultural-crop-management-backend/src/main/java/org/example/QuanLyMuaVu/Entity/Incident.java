package org.example.QuanLyMuaVu.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.IncidentSeverity;
import org.example.QuanLyMuaVu.Enums.IncidentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Integer id;

    @ManyToOne
    @JoinColumn(name = "season_id", nullable = false)
    Season season;

    @ManyToOne
    @JoinColumn(name = "reported_by")
    User reportedBy;

    @Column(name = "incident_type", length = 50)
    String incidentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20)
    IncidentSeverity severity;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    IncidentStatus status;

    @Column(name = "deadline")
    LocalDate deadline;

    @Column(name = "resolved_at")
    LocalDateTime resolvedAt;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    LocalDateTime createdAt;
}

