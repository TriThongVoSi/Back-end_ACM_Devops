package org.example.QuanLyMuaVu.Service.Admin;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Request.AdminUserWarningRequest;
import org.example.QuanLyMuaVu.DTO.Response.AdminUserWarningResponse;
import org.example.QuanLyMuaVu.Entity.Notification;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Entity.UserWarning;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.Enums.UserWarningDecision;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.NotificationRepository;
import org.example.QuanLyMuaVu.Repository.UserRepository;
import org.example.QuanLyMuaVu.Repository.UserWarningRepository;
import org.example.QuanLyMuaVu.Util.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminUserWarningService {

    private static final DateTimeFormatter LOCK_UNTIL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    NotificationRepository notificationRepository;
    UserRepository userRepository;
    UserWarningRepository userWarningRepository;
    CurrentUserService currentUserService;

    @Transactional
    public AdminUserWarningResponse warnUser(Long userId, AdminUserWarningRequest request) {
        if (request == null || !StringUtils.hasText(request.getDescription())
                || !StringUtils.hasText(request.getDecision())) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        UserWarningDecision decision = parseDecision(request.getDecision());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        User admin = currentUserService.getCurrentUser();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lockUntil = null;
        boolean updateUser = false;

        switch (decision) {
            case WARNING -> {
                // No account state change for warnings.
            }
            case LOCK_1_DAY -> {
                lockUntil = now.plusDays(1);
                updateUser = true;
                user.setStatus(UserStatus.LOCKED);
                user.setLockedUntil(lockUntil);
            }
            case LOCK_PERMANENT -> {
                updateUser = true;
                user.setStatus(UserStatus.LOCKED);
                user.setLockedUntil(null);
            }
        }

        if (updateUser) {
            userRepository.save(user);
        }

        String description = request.getDescription().trim();
        UserWarning warning = UserWarning.builder()
                .user(user)
                .createdBy(admin)
                .decision(decision)
                .description(description)
                .createdAt(now)
                .lockUntil(lockUntil)
                .build();
        UserWarning savedWarning = userWarningRepository.save(warning);

        Notification notification = Notification.builder()
                .user(user)
                .title(buildTitle(decision))
                .message(buildMessage(decision, description, lockUntil))
                .createdAt(now)
                .build();
        notificationRepository.save(notification);

        return AdminUserWarningResponse.builder()
                .id(savedWarning.getId())
                .userId(user.getId())
                .decision(savedWarning.getDecision().name())
                .description(savedWarning.getDescription())
                .createdAt(savedWarning.getCreatedAt())
                .lockUntil(savedWarning.getLockUntil())
                .userStatus(user.getStatus() != null ? user.getStatus().name() : null)
                .build();
    }

    private UserWarningDecision parseDecision(String decision) {
        try {
            UserWarningDecision parsed = UserWarningDecision.fromCode(decision);
            if (parsed == null) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            return parsed;
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private String buildTitle(UserWarningDecision decision) {
        return switch (decision) {
            case WARNING -> "Account warning";
            case LOCK_1_DAY -> "Account locked (1 day)";
            case LOCK_PERMANENT -> "Account locked permanently";
        };
    }

    private String buildMessage(UserWarningDecision decision, String description, LocalDateTime lockUntil) {
        StringBuilder message = new StringBuilder();
        if (StringUtils.hasText(description)) {
            message.append("Reason: ").append(description.trim());
        }

        String decisionLabel = switch (decision) {
            case WARNING -> "First warning";
            case LOCK_1_DAY -> "Account locked for 1 day";
            case LOCK_PERMANENT -> "Account locked permanently";
        };

        if (message.length() > 0) {
            message.append(". ");
        }
        message.append("Decision: ").append(decisionLabel);

        if (decision == UserWarningDecision.LOCK_1_DAY && lockUntil != null) {
            message.append(". Lock expires at ").append(lockUntil.format(LOCK_UNTIL_FORMAT));
        }

        return message.toString();
    }
}
