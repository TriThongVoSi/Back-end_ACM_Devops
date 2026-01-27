package org.example.QuanLyMuaVu.Filter;

import java.io.IOException;
import java.time.LocalDateTime;

import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.UserRepository;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter that checks if the authenticated user's account is still active.
 * 
 * This runs AFTER JWT authentication to verify the user hasn't been locked
 * by an admin since they logged in. If the user is locked, returns 403
 * with USER_LOCKED error code.
 * 
 * The frontend detects this error and shows a "Your account has been locked"
 * modal before redirecting to the sign-in page.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserStatusFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // Only check for authenticated requests
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();

                if (principal instanceof Jwt jwt) {
                    Long userId = extractUserId(jwt);

                    if (userId != null) {
                        // Check user status in database
                        User user = userRepository.findById(userId).orElse(null);

                        if (user != null && user.getStatus() == UserStatus.LOCKED) {
                            LocalDateTime lockedUntil = user.getLockedUntil();
                            if (lockedUntil != null && !lockedUntil.isAfter(LocalDateTime.now())) {
                                user.setStatus(UserStatus.ACTIVE);
                                user.setLockedUntil(null);
                                userRepository.save(user);
                            } else {
                                log.warn("Blocked request from locked user: {} (ID: {})",
                                        user.getUsername(), userId);

                                // Return 403 with USER_LOCKED error
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write(buildLockedErrorResponse());
                                return;
                            }
                        }
                    }
                }
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Error in UserStatusFilter: {}", e.getMessage());
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Extract user ID from JWT claims.
     */
    private Long extractUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaim("user_id");
        if (userIdClaim instanceof Number num) {
            return num.longValue();
        }
        if (userIdClaim instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                log.warn("Cannot parse user_id from JWT: {}", str);
            }
        }
        return null;
    }

    /**
     * Build JSON error response for locked user.
     */
    private String buildLockedErrorResponse() {
        return """
                {
                    "code": "USER_LOCKED",
                    "message": "Tài khoản của bạn đã bị khóa do vi phạm chính sách hệ thống. Vui lòng liên hệ quản trị viên để được hỗ trợ."
                }
                """;
    }

    /**
     * Skip filter for public endpoints.
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/api/v1/public/")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs");
    }
}
