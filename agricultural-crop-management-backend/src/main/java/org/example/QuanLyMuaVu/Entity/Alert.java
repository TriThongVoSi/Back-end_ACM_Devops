package org.example.QuanLyMuaVu.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.AlertSeverity;
import org.example.QuanLyMuaVu.Enums.AlertStatus;
import org.example.QuanLyMuaVu.Enums.AlertType;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    AlertStatus status;

    @Column(name = "farm_id")
    Integer farmId;

    @Column(name = "season_id")
    Integer seasonId;

    @Column(name = "plot_id")
    Integer plotId;

    @Column(name = "crop_id")
    Integer cropId;

    @Column(name = "title", length = 255)
    String title;

    @Column(name = "message", columnDefinition = "TEXT")
    String message;

    @Column(name = "suggested_action_type", length = 100)
    String suggestedActionType;

    @Column(name = "suggested_action_url", length = 500)
    String suggestedActionUrl;

    @Column(name = "recipient_farmer_ids", columnDefinition = "TEXT")
    String recipientFarmerIds;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "sent_at")
    LocalDateTime sentAt;
}
