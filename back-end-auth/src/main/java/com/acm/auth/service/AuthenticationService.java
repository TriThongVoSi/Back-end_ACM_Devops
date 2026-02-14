package com.acm.auth.service;

import java.text.ParseException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.nimbusds.jose.JOSEException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.acm.auth.dto.request.AuthenticationRequest;
import com.acm.auth.dto.request.IntrospectRequest;
import com.acm.auth.dto.request.LogoutRequest;
import com.acm.auth.dto.request.RefreshRequest;
import com.acm.auth.dto.response.AuthenticationResponse;
import com.acm.auth.dto.response.IntrospectResponse;
import com.acm.auth.entity.Role;
import com.acm.auth.entity.User;
import com.acm.auth.enums.UserStatus;
import com.acm.auth.exception.AppException;
import com.acm.auth.exception.ErrorCode;
import com.acm.auth.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    /**
     * Authenticate user by identifier (email OR username) and password.
     */
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        String identifier = request.getEffectiveIdentifier();
        if (identifier == null || identifier.isBlank()) {
            log.warn("Authentication failed - no identifier provided");
            throw new AppException(ErrorCode.IDENTIFIER_REQUIRED);
        }

        log.info("Authentication attempt for identifier: {}", identifier);

        User user = userRepository
                .findByIdentifierWithRoles(identifier)
                .orElseThrow(() -> {
                    log.warn("Authentication failed - identifier not found: {}", identifier);
                    return new AppException(ErrorCode.INVALID_CREDENTIALS);
                });

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!authenticated) {
            log.warn("Authentication failed - invalid password for identifier: {}", identifier);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Check user status
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Authentication failed - user not active. Identifier: {}, Status: {}",
                    identifier, user.getStatus());
            if (user.getStatus() == UserStatus.LOCKED) {
                throw new AppException(ErrorCode.USER_LOCKED);
            } else if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
                throw new AppException(ErrorCode.USER_PENDING_VERIFICATION);
            } else if (user.getStatus() == UserStatus.INACTIVE) {
                throw new AppException(ErrorCode.USER_INACTIVE);
            }
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        if (CollectionUtils.isEmpty(user.getRoles())) {
            log.warn("Authentication failed - no roles assigned to user: {}", identifier);
            throw new AppException(ErrorCode.ROLE_MISSING);
        }

        String primaryRole = determinePrimaryRole(user);
        var token = jwtTokenService.generateToken(user, primaryRole);
        log.info("Authentication successful for identifier: {} - role: {}", identifier, primaryRole);

        return buildAuthResponse(user, primaryRole, token);
    }

    /**
     * Get current user info (for /api/v1/auth/me endpoint).
     */
    public AuthenticationResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user = userRepository.findByIdentifierWithRoles(user.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String primaryRole = determinePrimaryRole(user);
        return buildAuthResponse(user, primaryRole, null);
    }

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
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
        }
        return null;
    }

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        boolean isValid = true;

        try {
            jwtTokenService.verifyToken(token, false);
        } catch (AppException e) {
            isValid = false;
        }

        return IntrospectResponse.builder().valid(isValid).build();
    }

    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            var signToken = jwtTokenService.verifyToken(request.getToken(), true);
            String jit = signToken.getJWTClaimsSet().getJWTID();
            var expiryTime = signToken.getJWTClaimsSet().getExpirationTime();
            jwtTokenService.invalidateToken(jit, expiryTime);
            log.info("Token invalidated successfully - JIT: {}", jit);
        } catch (AppException exception) {
            log.info("Logout - Token already expired or invalid");
        }
    }

    public AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        var signedJWT = jwtTokenService.verifyToken(request.getToken(), true);

        var jit = signedJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        jwtTokenService.invalidateToken(jit, expiryTime);

        var email = signedJWT.getJWTClaimsSet().getClaim("email");
        String identifier = email != null ? email.toString() : signedJWT.getJWTClaimsSet().getSubject();

        var user = userRepository.findByIdentifierWithRoles(identifier)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        String primaryRole = determinePrimaryRole(user);
        var token = jwtTokenService.generateToken(user, primaryRole);

        return buildAuthResponse(user, primaryRole, token);
    }

    /**
     * Determines the primary role based on priority (highest priority wins).
     * This is now configurable via database - no need to modify code for new roles.
     */
    private Role determinePrimaryRoleEntity(User user) {
        return user.getRoles().stream()
                .max((r1, r2) -> Integer.compare(r1.getPriority(), r2.getPriority()))
                .orElse(null);
    }

    private String determinePrimaryRole(User user) {
        Role primaryRole = determinePrimaryRoleEntity(user);
        return primaryRole != null ? primaryRole.getCode() : null;
    }

    /**
     * Gets redirect path from the primary role entity.
     * This is now configurable via database - no need to modify code for new roles.
     */
    private String determineRedirectPath(User user) {
        Role primaryRole = determinePrimaryRoleEntity(user);
        return primaryRole != null ? primaryRole.getRedirectPath() : "/";
    }

    private AuthenticationResponse buildAuthResponse(User user, String primaryRole, String token) {
        AuthenticationResponse.ProfileInfo profile = AuthenticationResponse.ProfileInfo.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .joinedDate(user.getJoinedDate() != null ? user.getJoinedDate().toString() : null)
                .build();

        var builder = AuthenticationResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .roles(user.getRoles().stream().map(Role::getCode).toList())
                .role(primaryRole)
                .profile(profile)
                .redirectTo(determineRedirectPath(user));

        if (token != null) {
            builder = builder.token(token)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenService.getValidDuration());
        }

        return builder.build();
    }
}
