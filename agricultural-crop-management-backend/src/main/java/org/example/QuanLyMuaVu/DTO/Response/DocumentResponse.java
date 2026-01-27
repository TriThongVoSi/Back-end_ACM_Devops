package org.example.QuanLyMuaVu.DTO.Response;

import java.time.LocalDateTime;

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
public class DocumentResponse {
    Integer documentId;
    String title;
    String url;
    String description;
    String crop;
    String stage;
    String topic;
    String documentType;
    Integer viewCount;
    Boolean isPinned;
    Boolean isActive;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Boolean isFavorited;
}
