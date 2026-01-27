package org.example.QuanLyMuaVu.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Response.FarmerNotificationResponse;
import org.example.QuanLyMuaVu.Entity.Notification;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FarmerNotificationService {

    NotificationRepository notificationRepository;
    FarmAccessService farmAccessService;

    @Transactional(readOnly = true)
    public List<FarmerNotificationResponse> listMyNotifications() {
        User currentUser = farmAccessService.getCurrentUser();
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FarmerNotificationResponse markAsRead(Long notificationId) {
        User currentUser = farmAccessService.getCurrentUser();
        Notification notification = notificationRepository.findByIdAndUser_Id(notificationId, currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }
        Notification saved = notificationRepository.save(notification);
        return toResponse(saved);
    }

    private FarmerNotificationResponse toResponse(Notification notification) {
        return FarmerNotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .link(notification.getLink())
                .alertId(notification.getAlertId())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
