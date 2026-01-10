package org.example.QuanLyMuaVu.DTO.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

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
    Boolean isActive;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Boolean isFavorited;
}
