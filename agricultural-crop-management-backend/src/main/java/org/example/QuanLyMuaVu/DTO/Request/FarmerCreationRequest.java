package org.example.QuanLyMuaVu.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FarmerCreationRequest {

    @NotBlank(message = "USERNAME_BLANK")
    String username;

    @NotBlank(message = "PASSWORD_BLANK")
    @Size(min = 8, max = 64, message = "PASSWORD_LENGTH_INVALID")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,64}$", message = "PASSWORD_POLICY_INVALID")
    String password;
}
