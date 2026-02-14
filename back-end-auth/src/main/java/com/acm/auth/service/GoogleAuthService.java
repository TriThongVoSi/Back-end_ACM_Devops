package com.acm.auth.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.acm.auth.dto.response.AuthenticationResponse;
import com.acm.auth.entity.Role;
import com.acm.auth.entity.User;
import com.acm.auth.enums.UserStatus;
import com.acm.auth.exception.AppException;
import com.acm.auth.exception.ErrorCode;
import com.acm.auth.repository.RoleRepository;
import com.acm.auth.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenService jwtTokenService;

    @Value("${google.client-id}")
    private String googleClientId;

    /**
     * Authenticate user via Google ID token.
     * Verifies the token, finds or creates the user, and returns a JWT.
     */
    @Transactional
    public AuthenticationResponse authenticateWithGoogle(String idTokenString) {
        // 1. Verify Google ID Token
        GoogleIdToken.Payload payload = verifyGoogleToken(idTokenString);

        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String fullName = (String) payload.get("name");

        log.info("Google authentication for email: {}, googleId: {}", email, googleId);

        // 2. Find or create user
        User user = findOrCreateUser(googleId, email, fullName);

        // 3. Check user status
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Google auth failed - user not active. Email: {}, Status: {}", email, user.getStatus());
            if (user.getStatus() == UserStatus.LOCKED) {
                throw new AppException(ErrorCode.USER_LOCKED);
            } else if (user.getStatus() == UserStatus.INACTIVE) {
                throw new AppException(ErrorCode.USER_INACTIVE);
            }
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        // 4. Ensure roles exist
        if (CollectionUtils.isEmpty(user.getRoles())) {
            log.warn("Google auth - no roles assigned to user: {}", email);
            throw new AppException(ErrorCode.ROLE_MISSING);
        }

        // 5. Generate JWT and return response
        String primaryRole = determinePrimaryRole(user);
        String token = jwtTokenService.generateToken(user, primaryRole);

        log.info("Google authentication successful for: {} - role: {}", email, primaryRole);

        return buildAuthResponse(user, primaryRole, token);
    }

    /**
     * Verify Google ID Token using Google API Client.
     */
    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                log.warn("Google ID token verification failed - invalid token");
                throw new AppException(ErrorCode.GOOGLE_AUTH_FAILED);
            }

            return idToken.getPayload();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google ID token verification error", e);
            throw new AppException(ErrorCode.GOOGLE_AUTH_FAILED, "Failed to verify Google token: " + e.getMessage());
        }
    }

    /**
     * Find existing user by googleId or email, or create a new one.
     */
    private User findOrCreateUser(String googleId, String email, String fullName) {
        // Try to find by Google ID first
        var byGoogleId = userRepository.findByGoogleIdWithRoles(googleId);
        if (byGoogleId.isPresent()) {
            return byGoogleId.get();
        }

        // Try to find by email (existing user linking Google account)
        var byEmail = userRepository.findByIdentifierWithRoles(email);
        if (byEmail.isPresent()) {
            User existingUser = byEmail.get();
            // Link Google ID to existing account
            existingUser.setGoogleId(googleId);
            if (existingUser.getFullName() == null || existingUser.getFullName().isBlank()) {
                existingUser.setFullName(fullName);
            }
            return userRepository.save(existingUser);
        }

        // Create new user
        return createGoogleUser(googleId, email, fullName);
    }

    /**
     * Create a new user from Google account info.
     */
    private User createGoogleUser(String googleId, String email, String fullName) {
        Role userRole = roleRepository.findByCode("USER")
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_ERROR, "Default role USER not found"));

        // Generate a unique username from email
        String username = email.split("@")[0];
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = email.split("@")[0] + suffix;
            suffix++;
        }

        User user = User.builder()
                .googleId(googleId)
                .email(email)
                .username(username)
                .fullName(fullName)
                .password(null) // No password for Google-only users
                .status(UserStatus.ACTIVE) // Google users are automatically verified
                .roles(Set.of(userRole))
                .joinedDate(LocalDateTime.now())
                .build();

        user = userRepository.save(user);
        log.info("New Google user created: {} ({})", email, username);
        return user;
    }

    private String determinePrimaryRole(User user) {
        return user.getRoles().stream()
                .max((r1, r2) -> Integer.compare(r1.getPriority(), r2.getPriority()))
                .map(Role::getCode)
                .orElse(null);
    }

    private String determineRedirectPath(User user) {
        return user.getRoles().stream()
                .max((r1, r2) -> Integer.compare(r1.getPriority(), r2.getPriority()))
                .map(Role::getRedirectPath)
                .orElse("/dashboard");
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

        return AuthenticationResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenService.getValidDuration())
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .roles(user.getRoles().stream().map(Role::getCode).toList())
                .role(primaryRole)
                .profile(profile)
                .redirectTo(determineRedirectPath(user))
                .build();
    }
}
