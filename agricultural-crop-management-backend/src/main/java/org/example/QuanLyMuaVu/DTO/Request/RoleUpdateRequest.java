package org.example.QuanLyMuaVu.DTO.Request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * DTO for updating an existing role.
 * Code is immutable after creation, only name and description can be updated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleUpdateRequest {
    String name;
    String description;
}
