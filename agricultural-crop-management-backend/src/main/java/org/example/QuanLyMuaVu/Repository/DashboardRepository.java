package org.example.QuanLyMuaVu.Repository;

import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DashboardRepository extends JpaRepository<org.example.QuanLyMuaVu.Entity.Season, Integer> {

    @Query("select r.code as role, count(u.id) as total " +
            "from User u join u.roles r " +
            "group by r.code")
    List<UserRoleCountProjection> countUsersByRole();

    @Query("select u.status as status, count(u.id) as total " +
            "from User u " +
            "group by u.status")
    List<UserStatusCountProjection> countUsersByStatus();

    @Query("select s.status as status, count(s.id) as total " +
            "from Season s " +
            "group by s.status")
    List<SeasonStatusCountProjection> countSeasonsByStatus();

    @Query("select s.id as seasonId, s.seasonName as seasonName, f.name as farmName, p.plotName as plotName, " +
            "s.status as status, " +
            "count(distinct i.id) as incidentCount, " +
            "count(distinct t.id) as overdueTaskCount, " +
            "(count(distinct i.id) + count(distinct t.id)) as riskScore " +
            "from Season s " +
            "join s.plot p " +
            "join p.farm f " +
            "left join Incident i on i.season = s " +
            "left join Task t on t.season = s and t.status = :overdueStatus " +
            "group by s.id, s.seasonName, f.name, p.plotName, s.status " +
            "order by (count(distinct i.id) + count(distinct t.id)) desc")
    List<RiskySeasonProjection> findRiskySeasons(@Param("overdueStatus") TaskStatus overdueStatus, Pageable pageable);

    @Query(value = "select f.farm_id as farmId, f.farm_name as farmName, " +
            "sum(case when sl.expiry_date < :today then 1 else 0 end) as expiredCount, " +
            "sum(case when sl.expiry_date >= :today and sl.expiry_date <= :cutoff then 1 else 0 end) as expiringSoonCount, "
            +
            "sum(case when sl.expiry_date <= :cutoff then 1 else 0 end) as totalAtRisk " +
            "from supply_lots sl " +
            "join ( " +
            "    select distinct sm.supply_lot_id, w.farm_id " +
            "    from stock_movements sm " +
            "    join warehouses w on w.id = sm.warehouse_id " +
            ") lot_farm on lot_farm.supply_lot_id = sl.id " +
            "join farms f on f.farm_id = lot_farm.farm_id " +
            "where sl.expiry_date is not null and sl.expiry_date <= :cutoff " +
            "group by f.farm_id, f.farm_name " +
            "order by expiredCount desc, expiringSoonCount desc", nativeQuery = true)
    List<InventoryHealthProjection> findInventoryHealth(@Param("today") LocalDate today,
            @Param("cutoff") LocalDate cutoff);

    interface UserRoleCountProjection {
        String getRole();

        Long getTotal();
    }

    interface UserStatusCountProjection {
        UserStatus getStatus();

        Long getTotal();
    }

    interface SeasonStatusCountProjection {
        SeasonStatus getStatus();

        Long getTotal();
    }

    interface RiskySeasonProjection {
        Integer getSeasonId();

        String getSeasonName();

        String getFarmName();

        String getPlotName();

        SeasonStatus getStatus();

        Long getIncidentCount();

        Long getOverdueTaskCount();

        Long getRiskScore();
    }

    interface InventoryHealthProjection {
        Integer getFarmId();

        String getFarmName();

        Long getExpiredCount();

        Long getExpiringSoonCount();

        Long getTotalAtRisk();
    }
}
