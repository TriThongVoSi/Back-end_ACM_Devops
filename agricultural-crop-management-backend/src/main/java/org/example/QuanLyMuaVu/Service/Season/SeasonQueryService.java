package org.example.QuanLyMuaVu.Service.Season;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.DTO.Response.MySeasonResponse;
import org.example.QuanLyMuaVu.DTO.Response.SeasonDetailResponse;
import org.example.QuanLyMuaVu.DTO.Response.SeasonResponse;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Mapper.SeasonMapper;
import org.example.QuanLyMuaVu.Repository.SeasonRepository;
import org.example.QuanLyMuaVu.Service.FarmAccessService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for Season query and search operations.
 * Single Responsibility: Read/query operations only.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class SeasonQueryService {

    SeasonRepository seasonRepository;
    SeasonMapper seasonMapper;
    FarmAccessService farmAccessService;

    /**
     * Get a minimal list of seasons for the current farmer (for dropdown
     * selectors).
     * Returns all accessible seasons with minimal fields.
     */
    public List<MySeasonResponse> getMySeasons() {
        User currentUser = farmAccessService.getCurrentUser();
        List<Integer> accessibleFarmIds = farmAccessService.getAccessibleFarmIdsForCurrentUser();

        LinkedHashSet<Season> allSeasons = new LinkedHashSet<>();

        if (!accessibleFarmIds.isEmpty()) {
            allSeasons.addAll(seasonRepository.findAllByPlot_Farm_IdIn(accessibleFarmIds));
        }
        allSeasons.addAll(seasonRepository.findAllByPlot_User(currentUser));

        return allSeasons.stream()
                .sorted((s1, s2) -> Integer.compare(
                        s2.getId() != null ? s2.getId() : 0,
                        s1.getId() != null ? s1.getId() : 0))
                .map(season -> MySeasonResponse.builder()
                        .seasonId(season.getId())
                        .seasonName(season.getSeasonName())
                        .startDate(season.getStartDate())
                        .endDate(season.getEndDate())
                        .plannedHarvestDate(season.getPlannedHarvestDate())
                        .status(season.getStatus() != null ? season.getStatus().name() : null)
                        .budgetAmount(season.getBudgetAmount())
                        .build())
                .toList();
    }

    /**
     * Search seasons for the current farmer with filters.
     */
    public PageResponse<SeasonResponse> searchMySeasons(
            Integer plotId,
            Integer cropId,
            String status,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {

        User currentUser = farmAccessService.getCurrentUser();
        List<Integer> accessibleFarmIds = farmAccessService.getAccessibleFarmIdsForCurrentUser();

        List<Season> all = new ArrayList<>();
        if (!accessibleFarmIds.isEmpty()) {
            all.addAll(seasonRepository.findAllByPlot_Farm_IdIn(accessibleFarmIds));
        }
        all.addAll(seasonRepository.findAllByPlot_User(currentUser));

        // De-duplicate by season id while preserving order
        Map<Integer, Season> byId = new LinkedHashMap<>();
        for (Season season : all) {
            if (season.getId() != null) {
                byId.putIfAbsent(season.getId(), season);
            }
        }
        all = new ArrayList<>(byId.values());

        SeasonStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = SeasonStatus.fromCode(status);
            } catch (IllegalArgumentException ex) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }

        final Integer plotIdFilter = plotId;
        final Integer cropIdFilter = cropId;
        final SeasonStatus statusFilterFinal = statusFilter;
        final LocalDate fromDate = from;
        final LocalDate toDate = to;

        List<SeasonResponse> filtered = all.stream()
                .filter(season -> plotIdFilter == null
                        || (season.getPlot() != null && plotIdFilter.equals(season.getPlot().getId())))
                .filter(season -> cropIdFilter == null
                        || (season.getCrop() != null && cropIdFilter.equals(season.getCrop().getId())))
                .filter(season -> statusFilterFinal == null || statusFilterFinal.equals(season.getStatus()))
                .filter(season -> filterByDateRange(season, fromDate, toDate))
                .sorted((s1, s2) -> Integer.compare(
                        s2.getId() != null ? s2.getId() : 0,
                        s1.getId() != null ? s1.getId() : 0))
                .map(seasonMapper::toResponse)
                .toList();

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, filtered.size());
        List<SeasonResponse> pageItems = fromIndex >= filtered.size() ? List.of()
                : filtered.subList(fromIndex, toIndex);
        Page<SeasonResponse> pageData = new PageImpl<>(pageItems, pageable, filtered.size());

        return PageResponse.of(pageData, pageItems);
    }

    /**
     * Get season detail for the current farmer.
     */
    public SeasonDetailResponse getSeasonForCurrentFarmer(Integer id) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        farmAccessService.assertCurrentUserCanAccessSeason(season);
        return seasonMapper.toDetailResponse(season);
    }

    /**
     * BR20/BR24/BR28: Get season by ID for confirmation screen display.
     */
    public Season getSeasonById(Integer id) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        farmAccessService.assertCurrentUserCanAccessSeason(season);
        return season;
    }

    /**
     * BR17: Search seasons by keyword for Text_change() handler.
     */
    public List<SeasonResponse> searchSeasonsByKeyword(String keyword) {
        User currentUser = farmAccessService.getCurrentUser();
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        List<Season> results = seasonRepository.searchByKeywordAndUserId(keyword.trim(), currentUser.getId());
        return results.stream()
                .map(seasonMapper::toResponse)
                .toList();
    }

    /**
     * Helper method to filter seasons by date range.
     */
    private boolean filterByDateRange(Season season, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null && toDate == null) {
            return true;
        }
        LocalDate sStart = season.getStartDate();
        LocalDate sEnd = season.getEndDate();
        if (sEnd == null) {
            sEnd = sStart;
        }
        LocalDate rangeStart = fromDate != null ? fromDate : sStart;
        LocalDate rangeEnd = toDate != null ? toDate : sEnd;
        return !sEnd.isBefore(rangeStart) && !sStart.isAfter(rangeEnd);
    }
}
