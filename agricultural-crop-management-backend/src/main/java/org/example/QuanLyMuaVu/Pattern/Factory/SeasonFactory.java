package org.example.QuanLyMuaVu.Pattern.Factory;

import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Request.CreateSeasonRequest;
import org.example.QuanLyMuaVu.Entity.Crop;
import org.example.QuanLyMuaVu.Entity.Plot;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Entity.Variety;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Pattern.Strategy.StatusTransitionStrategy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Factory Method Pattern: Season Factory.
 * <p>
 * Creates Season entities with proper defaults and validation.
 * Responsibilities:
 * - Generate season name if not provided
 * - Set initial status via Strategy pattern
 * - Populate audit fields
 * - Handle optional fields gracefully
 */
@Component
@RequiredArgsConstructor
public class SeasonFactory implements EntityFactory<Season, CreateSeasonRequest> {

    private final StatusTransitionStrategy<SeasonStatus> statusStrategy;

    @Override
    public Season create(CreateSeasonRequest request, User creator) {
        Season season = new Season();

        // Generate season name if not provided
        String seasonName = request.getSeasonName();
        if (seasonName == null || seasonName.isBlank()) {
            seasonName = generateSeasonName(request.getStartDate());
        }
        season.setSeasonName(seasonName);

        // Use Strategy for initial status
        season.setStatus(statusStrategy.getInitialStatus());

        // Set dates
        season.setStartDate(request.getStartDate());
        season.setPlannedHarvestDate(request.getPlannedHarvestDate());
        season.setEndDate(request.getEndDate());

        // Set optional fields
        if (request.getExpectedYieldKg() != null) {
            season.setExpectedYieldKg(request.getExpectedYieldKg());
        }
        if (request.getInitialPlantCount() != null) {
            season.setInitialPlantCount(request.getInitialPlantCount());
            season.setCurrentPlantCount(request.getInitialPlantCount());
        }
        season.setNotes(request.getNotes());

        // Note: Season entity doesn't have createdBy field in current schema
        // If audit tracking is needed, consider adding it to the entity

        return season;
    }

    /**
     * Creates a Season with pre-fetched entities (from validation context).
     */
    public Season createWithEntities(
            CreateSeasonRequest request,
            User creator,
            Plot plot,
            Crop crop,
            Variety variety) {

        Season season = create(request, creator);
        season.setPlot(plot);
        season.setCrop(crop);
        season.setVariety(variety);
        return season;
    }

    /**
     * Generates a default season name based on the start date.
     * Format: "Season {Quarter} {Year}" (e.g., "Season Q1 2025")
     */
    private String generateSeasonName(LocalDate startDate) {
        if (startDate == null) {
            return "New Season";
        }

        int month = startDate.getMonthValue();
        int quarter = (month - 1) / 3 + 1;
        int year = startDate.getYear();

        return String.format("Season Q%d %d", quarter, year);
    }
}
