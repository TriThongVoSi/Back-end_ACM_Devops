package org.example.QuanLyMuaVu.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Request.UserPreferencesUpdateRequest;
import org.example.QuanLyMuaVu.DTO.Response.UserPreferencesResponse;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Entity.UserPreferences;
import org.example.QuanLyMuaVu.Enums.CurrencyCode;
import org.example.QuanLyMuaVu.Enums.WeightUnit;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.UserPreferencesRepository;
import org.example.QuanLyMuaVu.Util.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class UserPreferencesService {

    UserPreferencesRepository userPreferencesRepository;
    CurrentUserService currentUserService;

    public UserPreferencesResponse getMyPreferences() {
        User user = currentUserService.getCurrentUser();
        UserPreferences preferences = resolveOrCreatePreferences(user);
        return toResponse(preferences);
    }

    public UserPreferencesResponse updateMyPreferences(UserPreferencesUpdateRequest request) {
        User user = currentUserService.getCurrentUser();
        UserPreferences preferences = resolveOrCreatePreferences(user);

        if (request.getCurrency() != null) {
            preferences.setCurrencyCode(parseCurrency(request.getCurrency()));
        }

        if (request.getWeightUnit() != null) {
            preferences.setWeightUnit(parseWeightUnit(request.getWeightUnit()));
        }

        if (request.getLocale() != null) {
            String trimmed = request.getLocale().trim();
            if (!trimmed.isEmpty()) {
                preferences.setLocale(trimmed);
            }
        }

        UserPreferences saved = userPreferencesRepository.save(preferences);
        return toResponse(saved);
    }

    private UserPreferences resolveOrCreatePreferences(User user) {
        return userPreferencesRepository.findByUser_Id(user.getId())
                .orElseGet(() -> userPreferencesRepository.save(UserPreferences.builder().user(user).build()));
    }

    private CurrencyCode parseCurrency(String value) {
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        try {
            return CurrencyCode.fromCode(normalized);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private WeightUnit parseWeightUnit(String value) {
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        try {
            return WeightUnit.fromCode(normalized);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private UserPreferencesResponse toResponse(UserPreferences preferences) {
        return UserPreferencesResponse.builder()
                .currency(preferences.getCurrencyCode().getCode())
                .weightUnit(preferences.getWeightUnit().getCode())
                .locale(preferences.getLocale())
                .build();
    }
}
