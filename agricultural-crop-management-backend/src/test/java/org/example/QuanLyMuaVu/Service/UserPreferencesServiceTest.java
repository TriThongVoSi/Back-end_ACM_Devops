package org.example.QuanLyMuaVu.Service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPreferencesServiceTest {

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private UserPreferencesService userPreferencesService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).build();
    }

    @Test
    @DisplayName("Get preferences - auto-creates defaults on first access")
    void getMyPreferences_CreatesDefaultWhenMissing() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userPreferencesRepository.findByUser_Id(user.getId()))
                .thenReturn(Optional.empty());
        when(userPreferencesRepository.save(any(UserPreferences.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPreferencesResponse response = userPreferencesService.getMyPreferences();

        assertNotNull(response);
        assertEquals("VND", response.getCurrency());
        assertEquals("KG", response.getWeightUnit());
        assertEquals("vi-VN", response.getLocale());
    }

    @Test
    @DisplayName("Update preferences - partial update keeps existing values")
    void updateMyPreferences_PartialUpdate() {
        UserPreferences existing = UserPreferences.builder()
                .id(10L)
                .user(user)
                .currencyCode(CurrencyCode.VND)
                .weightUnit(WeightUnit.KG)
                .locale("vi-VN")
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userPreferencesRepository.findByUser_Id(user.getId()))
                .thenReturn(Optional.of(existing));
        when(userPreferencesRepository.save(any(UserPreferences.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPreferencesUpdateRequest request = UserPreferencesUpdateRequest.builder()
                .currency("USD")
                .build();

        UserPreferencesResponse response = userPreferencesService.updateMyPreferences(request);

        assertEquals("USD", response.getCurrency());
        assertEquals("KG", response.getWeightUnit());
        assertEquals("vi-VN", response.getLocale());
    }

    @Test
    @DisplayName("Update preferences - invalid currency throws BAD_REQUEST")
    void updateMyPreferences_InvalidCurrency() {
        UserPreferences existing = UserPreferences.builder()
                .id(10L)
                .user(user)
                .currencyCode(CurrencyCode.VND)
                .weightUnit(WeightUnit.KG)
                .locale("vi-VN")
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userPreferencesRepository.findByUser_Id(user.getId()))
                .thenReturn(Optional.of(existing));

        UserPreferencesUpdateRequest request = UserPreferencesUpdateRequest.builder()
                .currency("ABC")
                .build();

        AppException exception = assertThrows(AppException.class,
                () -> userPreferencesService.updateMyPreferences(request));

        assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
    }

    @Test
    @DisplayName("Update preferences - invalid weight unit throws BAD_REQUEST")
    void updateMyPreferences_InvalidWeightUnit() {
        UserPreferences existing = UserPreferences.builder()
                .id(10L)
                .user(user)
                .currencyCode(CurrencyCode.VND)
                .weightUnit(WeightUnit.KG)
                .locale("vi-VN")
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userPreferencesRepository.findByUser_Id(user.getId()))
                .thenReturn(Optional.of(existing));

        UserPreferencesUpdateRequest request = UserPreferencesUpdateRequest.builder()
                .weightUnit("LB")
                .build();

        AppException exception = assertThrows(AppException.class,
                () -> userPreferencesService.updateMyPreferences(request));

        assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
    }
}
