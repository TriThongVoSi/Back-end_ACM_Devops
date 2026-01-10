package org.example.QuanLyMuaVu.DTO.Request;

import jakarta.validation.constraints.Email;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {

    // Username update (must be unique)
    String username;

    @Email(message = "INVALID_EMAIL_FORMAT")
    String email;

    String fullName;

    String phone;

    // List of role codes (e.g., ["FARMER", "BUYER"])
    List<String> roles;

    // Status update (ACTIVE, INACTIVE, LOCKED)
    String status;
}
