package org.example.QuanLyMuaVu.Pattern.Observer;

import lombok.Getter;
import org.example.QuanLyMuaVu.Entity.Season;

/**
 * Observer Pattern: Season Created Event.
 * <p>
 * Published when a new season is successfully created.
 * Listeners can use this to:
 * - Auto-generate default tasks from templates
 * - Send notifications to farm managers
 * - Update dashboards/statistics
 */
@Getter
public class SeasonCreatedEvent extends DomainEvent {

    private final Integer seasonId;
    private final String seasonName;
    private final Integer plotId;
    private final Integer cropId;

    public SeasonCreatedEvent(Season season) {
        super("Season", season.getId() != null ? season.getId().toString() : "unknown");
        this.seasonId = season.getId();
        this.seasonName = season.getSeasonName();
        this.plotId = season.getPlot() != null ? season.getPlot().getId() : null;
        this.cropId = season.getCrop() != null ? season.getCrop().getId() : null;
    }

    @Override
    public String getEventType() {
        return "SEASON_CREATED";
    }
}
