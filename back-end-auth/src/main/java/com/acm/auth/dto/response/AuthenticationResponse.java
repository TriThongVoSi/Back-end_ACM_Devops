package com.acm.auth.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {
    private String token;
    private String tokenType;
    private Long expiresIn;
    private Long userId;
    private String email;
    private String username;
    private List<String> roles;
    private String role;
    private ProfileInfo profile;
    private String redirectTo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileInfo {
        private Long id;
        private String fullName;
        private String email;
        private String phone;
        private String status;
        private String joinedDate;
    }
}
