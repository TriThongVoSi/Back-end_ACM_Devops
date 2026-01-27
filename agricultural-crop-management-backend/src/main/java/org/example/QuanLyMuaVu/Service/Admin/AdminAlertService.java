package org.example.QuanLyMuaVu.Service.Admin;

import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.DTO.Request.AlertSendRequest;
import org.example.QuanLyMuaVu.DTO.Response.AdminAlertResponse;
import org.example.QuanLyMuaVu.Entity.Alert;
import org.example.QuanLyMuaVu.Entity.Farm;
import org.example.QuanLyMuaVu.Entity.Notification;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Enums.AlertSeverity;
import org.example.QuanLyMuaVu.Enums.AlertStatus;
import org.example.QuanLyMuaVu.Enums.AlertType;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.AlertRepository;
import org.example.QuanLyMuaVu.Repository.FarmRepository;
import org.example.QuanLyMuaVu.Repository.NotificationRepository;
import org.example.QuanLyMuaVu.Repository.StockMovementRepository;
import org.example.QuanLyMuaVu.Repository.StockMovementRepository.InventoryLotRiskProjection;
import org.example.QuanLyMuaVu.Repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminAlertService {

    private static final int DEFAULT_WINDOW_DAYS = 30;

    AlertRepository alertRepository;
    StockMovementRepository stockMovementRepository;
    FarmRepository farmRepository;
    UserRepository userRepository;
    NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminAlertResponse> listAlerts(String type, String severity, String status, Integer farmId,
            Integer windowDays, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Alert> spec = buildSpecification(type, severity, status, farmId, windowDays);
        Page<Alert> alertPage = alertRepository.findAll(spec, pageable);

        Map<Integer, Farm> farmMap = loadFarms(alertPage.getContent());

        List<AdminAlertResponse> items = alertPage.getContent().stream()
                .map(alert -> toResponse(alert, farmMap))
                .collect(Collectors.toList());

        return PageResponse.of(alertPage, items);
    }

    @Transactional
    public List<AdminAlertResponse> refreshAlerts(Integer windowDays) {
        int safeWindowDays = windowDays != null ? windowDays : DEFAULT_WINDOW_DAYS;
        if (safeWindowDays < 0) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusNanos(1);

        List<InventoryLotRiskProjection> lots = stockMovementRepository.findOnHandLotsWithDetails(null, null);
        Map<AlertKey, AlertAccumulator> accumulators = new HashMap<>();

        for (InventoryLotRiskProjection lot : lots) {
            if (lot.getExpiryDate() == null || lot.getFarmId() == null) {
                continue;
            }
            long daysToExpiry = ChronoUnit.DAYS.between(today, lot.getExpiryDate());
            AlertType alertType = null;
            if (daysToExpiry < 0) {
                alertType = AlertType.INVENTORY_EXPIRED;
            } else if (daysToExpiry <= safeWindowDays) {
                alertType = AlertType.INVENTORY_EXPIRING;
            }
            if (alertType == null) {
                continue;
            }

            AlertType resolvedType = alertType;
            AlertKey key = new AlertKey(lot.getFarmId(), resolvedType);
            AlertAccumulator acc = accumulators.computeIfAbsent(key,
                    k -> new AlertAccumulator(lot.getFarmId(), lot.getFarmName(), resolvedType));
            acc.count++;
            acc.minDaysToExpiry = Math.min(acc.minDaysToExpiry, daysToExpiry);
        }

        List<Alert> upserted = new ArrayList<>();
        for (AlertAccumulator acc : accumulators.values()) {
            AlertSeverity severity = resolveSeverity(acc.type, acc.minDaysToExpiry);
            String title = acc.type == AlertType.INVENTORY_EXPIRED
                    ? "Expired inventory detected"
                    : "Inventory expiring soon";
            String message = buildInventoryMessage(acc, safeWindowDays);
            String actionUrl = "/admin/inventory?farmId=" + acc.farmId + "&status=RISK&windowDays=" + safeWindowDays;

            Optional<Alert> existing = alertRepository.findFirstByFarmIdAndTypeAndCreatedAtBetween(
                    acc.farmId, acc.type, startOfDay, endOfDay);

            Alert alert = existing.orElseGet(Alert::new);
            alert.setFarmId(acc.farmId);
            alert.setType(acc.type);
            alert.setSeverity(severity);
            alert.setTitle(title);
            alert.setMessage(message);
            alert.setSuggestedActionType("INVENTORY");
            alert.setSuggestedActionUrl(actionUrl);
            if (alert.getCreatedAt() == null) {
                alert.setCreatedAt(LocalDateTime.now());
            }
            if (alert.getStatus() == null) {
                alert.setStatus(AlertStatus.NEW);
            }

            upserted.add(alertRepository.save(alert));
        }

        Map<Integer, Farm> farmMap = loadFarms(upserted);
        return upserted.stream()
                .map(alert -> toResponse(alert, farmMap))
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminAlertResponse sendAlert(Long alertId, AlertSendRequest request) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        validateChannel(request.getChannel());

        List<Long> recipientIds = resolveRecipients(alert, request);
        if (recipientIds.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        List<User> recipients = userRepository.findAllById(recipientIds);
        if (recipients.size() != recipientIds.size()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        String link = buildFarmerLink(alert);
        List<Notification> notifications = recipients.stream()
                .map(user -> Notification.builder()
                        .user(user)
                        .title(alert.getTitle())
                        .message(alert.getMessage())
                        .link(link)
                        .alertId(alert.getId())
                        .createdAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());

        notificationRepository.saveAll(notifications);

        alert.setStatus(AlertStatus.SENT);
        alert.setSentAt(LocalDateTime.now());
        alert.setRecipientFarmerIds(joinRecipientIds(recipientIds));
        Alert saved = alertRepository.save(alert);

        Map<Integer, Farm> farmMap = loadFarms(List.of(saved));
        return toResponse(saved, farmMap);
    }

    @Transactional
    public AdminAlertResponse updateStatus(Long alertId, String status) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        AlertStatus newStatus = AlertStatus.fromCode(status);
        if (newStatus == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        alert.setStatus(newStatus);
        Alert saved = alertRepository.save(alert);
        Map<Integer, Farm> farmMap = loadFarms(List.of(saved));
        return toResponse(saved, farmMap);
    }

    private Specification<Alert> buildSpecification(String type, String severity, String status, Integer farmId,
            Integer windowDays) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(type) && !"ALL".equalsIgnoreCase(type)) {
                try {
                    AlertType typeEnum = AlertType.fromCode(type);
                    predicates.add(cb.equal(root.get("type"), typeEnum));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid alert type filter: {}", type);
                }
            }

            if (StringUtils.hasText(severity) && !"ALL".equalsIgnoreCase(severity)) {
                try {
                    AlertSeverity severityEnum = AlertSeverity.fromCode(severity);
                    predicates.add(cb.equal(root.get("severity"), severityEnum));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid alert severity filter: {}", severity);
                }
            }

            if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
                try {
                    AlertStatus statusEnum = AlertStatus.fromCode(status);
                    predicates.add(cb.equal(root.get("status"), statusEnum));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid alert status filter: {}", status);
                }
            }

            if (farmId != null) {
                predicates.add(cb.equal(root.get("farmId"), farmId));
            }

            if (windowDays != null && windowDays > 0) {
                LocalDateTime from = LocalDate.now().minusDays(windowDays).atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AlertSeverity resolveSeverity(AlertType type, long minDaysToExpiry) {
        if (type == AlertType.INVENTORY_EXPIRED) {
            return AlertSeverity.CRITICAL;
        }
        if (minDaysToExpiry <= 7) {
            return AlertSeverity.HIGH;
        }
        if (minDaysToExpiry <= 30) {
            return AlertSeverity.MEDIUM;
        }
        return AlertSeverity.LOW;
    }

    private String buildInventoryMessage(AlertAccumulator acc, int windowDays) {
        if (acc.type == AlertType.INVENTORY_EXPIRED) {
            return acc.count + " lots have expired in " + acc.farmName + ". Review inventory immediately.";
        }
        return acc.count + " lots expire within " + windowDays + " days in " + acc.farmName + ".";
    }

    private void validateChannel(String channel) {
        if (channel == null) {
            return;
        }
        if (!"IN_APP".equalsIgnoreCase(channel)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private List<Long> resolveRecipients(Alert alert, AlertSendRequest request) {
        if (request == null || !StringUtils.hasText(request.getRecipientMode())) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        String mode = request.getRecipientMode().trim().toUpperCase();
        if ("ALL_FARMERS_IN_FARM".equals(mode)) {
            if (alert.getFarmId() == null) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            Farm farm = farmRepository.findById(alert.getFarmId())
                    .orElseThrow(() -> new AppException(ErrorCode.FARM_NOT_FOUND));
            if (farm.getUser() == null) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            return List.of(farm.getUser().getId());
        }

        if ("SELECTED".equals(mode)) {
            return request.getRecipientFarmerIds() != null ? request.getRecipientFarmerIds() : List.of();
        }

        throw new AppException(ErrorCode.BAD_REQUEST);
    }

    private String buildFarmerLink(Alert alert) {
        if (!StringUtils.hasText(alert.getSuggestedActionUrl())) {
            return null;
        }
        String url = alert.getSuggestedActionUrl();
        if (url.startsWith("/admin/")) {
            return "/farmer/" + url.substring("/admin/".length());
        }
        return url;
    }

    private Map<Integer, Farm> loadFarms(List<Alert> alerts) {
        Set<Integer> farmIds = alerts.stream()
                .map(Alert::getFarmId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (farmIds.isEmpty()) {
            return Map.of();
        }
        return farmRepository.findAllById(farmIds).stream()
                .collect(Collectors.toMap(Farm::getId, farm -> farm));
    }

    private AdminAlertResponse toResponse(Alert alert, Map<Integer, Farm> farmMap) {
        Farm farm = alert.getFarmId() != null ? farmMap.get(alert.getFarmId()) : null;
        return AdminAlertResponse.builder()
                .id(alert.getId())
                .type(alert.getType() != null ? alert.getType().name() : null)
                .severity(alert.getSeverity() != null ? alert.getSeverity().name() : null)
                .status(alert.getStatus() != null ? alert.getStatus().name() : null)
                .farmId(alert.getFarmId())
                .farmName(farm != null ? farm.getName() : null)
                .seasonId(alert.getSeasonId())
                .plotId(alert.getPlotId())
                .cropId(alert.getCropId())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .suggestedActionType(alert.getSuggestedActionType())
                .suggestedActionUrl(alert.getSuggestedActionUrl())
                .recipientFarmerIds(parseRecipientIds(alert.getRecipientFarmerIds()))
                .createdAt(alert.getCreatedAt())
                .sentAt(alert.getSentAt())
                .build();
    }

    private List<Long> parseRecipientIds(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        String[] parts = raw.split(",");
        List<Long> ids = new ArrayList<>();
        for (String part : parts) {
            try {
                ids.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
                // ignore invalid entries
            }
        }
        return ids;
    }

    private String joinRecipientIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private static class AlertKey {
        private final Integer farmId;
        private final AlertType type;

        private AlertKey(Integer farmId, AlertType type) {
            this.farmId = farmId;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AlertKey alertKey = (AlertKey) o;
            return farmId.equals(alertKey.farmId) && type == alertKey.type;
        }

        @Override
        public int hashCode() {
            int result = farmId.hashCode();
            result = 31 * result + type.hashCode();
            return result;
        }
    }

    private static class AlertAccumulator {
        private final Integer farmId;
        private final String farmName;
        private final AlertType type;
        private int count = 0;
        private long minDaysToExpiry = Long.MAX_VALUE;

        private AlertAccumulator(Integer farmId, String farmName, AlertType type) {
            this.farmId = farmId;
            this.farmName = farmName;
            this.type = type;
        }
    }
}
