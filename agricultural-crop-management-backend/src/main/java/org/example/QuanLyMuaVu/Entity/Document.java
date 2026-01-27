package org.example.QuanLyMuaVu.Entity;

import java.time.LocalDateTime;

import org.example.QuanLyMuaVu.Enums.DocumentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "documents")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    Integer id;

    @Column(name = "title", nullable = false, length = 255)
    String title;

    @Column(name = "url", nullable = false, length = 1000)
    String url;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "crop", length = 50)
    String crop;

    @Column(name = "stage", length = 50)
    String stage;

    @Column(name = "topic", length = 50)
    String topic;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_public", nullable = false)
    Boolean isPublic = true;

    @Column(name = "created_by")
    Long createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 50)
    @Builder.Default
    DocumentType documentType = DocumentType.GUIDE;

    @Column(name = "view_count")
    @Builder.Default
    Integer viewCount = 0;

    @Column(name = "is_pinned")
    @Builder.Default
    Boolean isPinned = false;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
