package org.example.QuanLyMuaVu.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Entity.Farm;
import org.example.QuanLyMuaVu.Entity.Plot;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Entity.Warehouse;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.FarmRepository;
import org.example.QuanLyMuaVu.Repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Centralized helper for farm-level authorization, ensuring that only farm
 * owners
 * can access farm/plot/season/warehouse resources.
 * <p>
 * This service is used by season, task, field log, expense, harvest, inventory,
 * quality and incident modules to enforce the ACM business rules on top of
 * RBAC.
 * <p>
 * Note: FarmMember functionality was removed as per DDL schema update.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FarmAccessService {

    FarmRepository farmRepository;
    UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // JWT subject contains user_id as a string, not username
        String userIdStr = authentication.getName();
        try {
            Long userId = Long.parseLong(userIdStr);
            return userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        } catch (NumberFormatException e) {
            // If it's not a number, try looking up by username (for backward compatibility)
            return userRepository.findByUsername(userIdStr)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        }
    }

    /**
     * Returns IDs of farms where the current user is the owner.
     */
    public List<Integer> getAccessibleFarmIdsForCurrentUser() {
        User currentUser = getCurrentUser();

        List<Farm> ownerFarms = farmRepository.findAllByUser(currentUser);

        return ownerFarms.stream()
                .map(Farm::getId)
                .filter(id -> id != null)
                .toList();
    }

    public void assertCurrentUserCanAccessFarm(Farm farm) {
        if (farm == null) {
            throw new AppException(ErrorCode.FARM_NOT_FOUND);
        }

        User currentUser = getCurrentUser();

        if (farm.getUser() != null && farm.getUser().getId().equals(currentUser.getId())) {
            return;
        }

        throw new AppException(ErrorCode.FORBIDDEN);
    }

    public void assertCurrentUserCanAccessPlot(Plot plot) {
        if (plot == null) {
            throw new AppException(ErrorCode.PLOT_NOT_FOUND);
        }

        Farm farm = plot.getFarm();
        if (farm != null) {
            assertCurrentUserCanAccessFarm(farm);
            return;
        }

        // Legacy fallback when plots are not linked to farms: require direct ownership.
        User currentUser = getCurrentUser();
        if (plot.getUser() != null && plot.getUser().getId().equals(currentUser.getId())) {
            return;
        }

        throw new AppException(ErrorCode.FORBIDDEN);
    }

    public void assertCurrentUserCanAccessSeason(Season season) {
        if (season == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }
        Plot plot = season.getPlot();
        if (plot == null) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        assertCurrentUserCanAccessPlot(plot);
    }

    public void assertCurrentUserCanAccessWarehouse(Warehouse warehouse) {
        if (warehouse == null) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        Farm farm = warehouse.getFarm();
        if (farm == null) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        assertCurrentUserCanAccessFarm(farm);
    }
}
