package org.example.QuanLyMuaVu.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "document_recent_opens")
public class DocumentRecentOpen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "document_id", nullable = false)
    Integer documentId;

    @Column(name = "opened_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    LocalDateTime openedAt;

    @PrePersist
    protected void onCreate() {
        openedAt = LocalDateTime.now();
    }
}
