package com.acm.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordVerifyOtpResponse {
    private String tempResetToken;
    private Long expiresInSeconds;
}
