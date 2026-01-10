package org.example.QuanLyMuaVu.Repository;

import org.example.QuanLyMuaVu.Entity.Incident;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Enums.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Integer>, JpaSpecificationExecutor<Incident> {

    List<Incident> findAllBySeason(Season season);

    /**
     * Count incidents by season and status for summary chips
     */
    long countBySeasonAndStatus(Season season, IncidentStatus status);

    /**
     * Find all seasons that have incidents reported by a specific user
     */
    @Query("SELECT DISTINCT i.season FROM Incident i WHERE i.reportedBy = :user")
    List<Season> findDistinctSeasonsByReportedBy(@Param("user") User user);

    /**
     * Count open incidents for farmer's seasons.
     * Used for dashboard alert count.
     */
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.season.plot.farm.user.id = :ownerId AND i.status IN :openStatuses")
    long countByFarmUserIdAndStatusIn(@Param("ownerId") Long ownerId,
            @Param("openStatuses") List<IncidentStatus> openStatuses);

    // ═══════════════════════════════════════════════════════════════
    // ADMIN QUERY METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Count incidents by status list (admin dashboard).
     */
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.status IN :statuses")
    long countByStatusIn(@Param("statuses") List<String> statuses);
}
