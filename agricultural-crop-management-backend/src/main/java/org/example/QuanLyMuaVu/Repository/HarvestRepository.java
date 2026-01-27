package org.example.QuanLyMuaVu.Repository;

import org.example.QuanLyMuaVu.DTO.Response.AdminReportProjections;
import org.example.QuanLyMuaVu.Entity.Harvest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Repository
public interface HarvestRepository extends JpaRepository<Harvest, Integer> {

    List<Harvest> findByHarvestDateBetween(LocalDate start, LocalDate end);

    List<Harvest> findAllBySeason_Id(Integer seasonId);

    List<Harvest> findAllBySeason_IdIn(Iterable<Integer> seasonIds);

    boolean existsBySeason_Id(Integer seasonId);

    @Query("SELECT COALESCE(SUM(h.quantity), 0) FROM Harvest h WHERE h.season.id = :seasonId")
    BigDecimal sumQuantityBySeasonId(@Param("seasonId") Integer seasonId);

    @Query("SELECT COALESCE(SUM(h.quantity * h.unit), 0) FROM Harvest h WHERE h.season.id = :seasonId")
    BigDecimal sumRevenueBySeasonId(@Param("seasonId") Integer seasonId);

    // ═══════════════════════════════════════════════════════════════
    // ADMIN AGGREGATION METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Sum harvest quantities for date range (admin dashboard).
     */
    @Query("SELECT COALESCE(SUM(h.quantity), 0) FROM Harvest h WHERE h.harvestDate BETWEEN :startDate AND :endDate")
    BigDecimal sumQuantityByHarvestDateBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Sum harvest quantity using report filters.
     */
    @Query("SELECT COALESCE(SUM(h.quantity), 0) FROM Harvest h " +
            "WHERE (:fromDate IS NULL OR h.harvestDate >= :fromDate) " +
            "AND (:toDate IS NULL OR h.harvestDate <= :toDate) " +
            "AND (:farmId IS NULL OR h.season.plot.farm.id = :farmId) " +
            "AND (:plotId IS NULL OR h.season.plot.id = :plotId) " +
            "AND (:cropId IS NULL OR h.season.crop.id = :cropId) " +
            "AND (:varietyId IS NULL OR h.season.variety.id = :varietyId)")
    BigDecimal sumQuantityByFilters(@Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("farmId") Integer farmId,
            @Param("plotId") Integer plotId,
            @Param("cropId") Integer cropId,
            @Param("varietyId") Integer varietyId);

    /**
     * Sum harvest revenue using report filters.
     */
    @Query("SELECT COALESCE(SUM(h.quantity * h.unit), 0) FROM Harvest h " +
            "WHERE (:fromDate IS NULL OR h.harvestDate >= :fromDate) " +
            "AND (:toDate IS NULL OR h.harvestDate <= :toDate) " +
            "AND (:farmId IS NULL OR h.season.plot.farm.id = :farmId) " +
            "AND (:plotId IS NULL OR h.season.plot.id = :plotId) " +
            "AND (:cropId IS NULL OR h.season.crop.id = :cropId) " +
            "AND (:varietyId IS NULL OR h.season.variety.id = :varietyId)")
    BigDecimal sumRevenueByFilters(@Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("farmId") Integer farmId,
            @Param("plotId") Integer plotId,
            @Param("cropId") Integer cropId,
            @Param("varietyId") Integer varietyId);

    /**
     * Count harvests with missing or zero unit price using report filters.
     */
    @Query("SELECT COUNT(h) FROM Harvest h " +
            "WHERE (h.unit IS NULL OR h.unit <= 0) " +
            "AND (:fromDate IS NULL OR h.harvestDate >= :fromDate) " +
            "AND (:toDate IS NULL OR h.harvestDate <= :toDate) " +
            "AND (:farmId IS NULL OR h.season.plot.farm.id = :farmId) " +
            "AND (:plotId IS NULL OR h.season.plot.id = :plotId) " +
            "AND (:cropId IS NULL OR h.season.crop.id = :cropId) " +
            "AND (:varietyId IS NULL OR h.season.variety.id = :varietyId)")
    long countMissingUnitPriceByFilters(@Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("farmId") Integer farmId,
            @Param("plotId") Integer plotId,
            @Param("cropId") Integer cropId,
            @Param("varietyId") Integer varietyId);

    /**
     * Sum harvest quantity grouped by season ID.
     */
    @Query("SELECT h.season.id AS seasonId, COALESCE(SUM(h.quantity), 0) AS totalQuantity " +
            "FROM Harvest h WHERE h.season.id IN :seasonIds GROUP BY h.season.id")
    List<AdminReportProjections.SeasonHarvestAgg> sumQuantityBySeasonIds(
            @Param("seasonIds") Set<Integer> seasonIds);

    /**
     * Sum revenue (quantity * unit) grouped by season ID.
     */
    @Query("SELECT h.season.id AS seasonId, " +
            "COALESCE(SUM(h.quantity), 0) AS totalQuantity, " +
            "COALESCE(SUM(h.quantity * h.unit), 0) AS totalRevenue " +
            "FROM Harvest h WHERE h.season.id IN :seasonIds GROUP BY h.season.id")
    List<AdminReportProjections.SeasonRevenueAgg> sumRevenueBySeasonIds(
            @Param("seasonIds") Set<Integer> seasonIds);

    /**
     * Yield aggregation by farm/plot/crop/variety for admin analytics.
     */
    @Query("SELECT s.plot.farm.id AS farmId, s.plot.farm.name AS farmName, " +
            "s.plot.id AS plotId, s.plot.plotName AS plotName, " +
            "s.crop.id AS cropId, s.crop.cropName AS cropName, " +
            "s.variety.id AS varietyId, s.variety.name AS varietyName, " +
            "COALESCE(SUM(h.quantity), 0) AS totalYield, " +
            "COUNT(h.id) AS harvestCount " +
            "FROM Harvest h JOIN h.season s " +
            "WHERE (:fromDate IS NULL OR h.harvestDate >= :fromDate) " +
            "AND (:toDate IS NULL OR h.harvestDate <= :toDate) " +
            "AND (:farmId IS NULL OR s.plot.farm.id = :farmId) " +
            "AND (:plotId IS NULL OR s.plot.id = :plotId) " +
            "AND (:cropId IS NULL OR s.crop.id = :cropId) " +
            "AND (:varietyId IS NULL OR s.variety.id = :varietyId) " +
            "GROUP BY s.plot.farm.id, s.plot.farm.name, s.plot.id, s.plot.plotName, " +
            "s.crop.id, s.crop.cropName, s.variety.id, s.variety.name")
    List<AdminReportProjections.YieldGroupAgg> sumYieldByGroup(@Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("farmId") Integer farmId,
            @Param("plotId") Integer plotId,
            @Param("cropId") Integer cropId,
            @Param("varietyId") Integer varietyId);

    /**
     * Revenue aggregation by crop and plot for admin analytics.
     */
    @Query("SELECT s.crop.id AS cropId, s.crop.cropName AS cropName, " +
            "s.plot.id AS plotId, s.plot.plotName AS plotName, " +
            "COALESCE(SUM(h.quantity), 0) AS totalQuantity, " +
            "COALESCE(SUM(h.quantity * h.unit), 0) AS totalRevenue " +
            "FROM Harvest h JOIN h.season s " +
            "WHERE (:fromDate IS NULL OR h.harvestDate >= :fromDate) " +
            "AND (:toDate IS NULL OR h.harvestDate <= :toDate) " +
            "AND (:farmId IS NULL OR s.plot.farm.id = :farmId) " +
            "AND (:plotId IS NULL OR s.plot.id = :plotId) " +
            "AND (:cropId IS NULL OR s.crop.id = :cropId) " +
            "AND (:varietyId IS NULL OR s.variety.id = :varietyId) " +
            "GROUP BY s.crop.id, s.crop.cropName, s.plot.id, s.plot.plotName")
    List<AdminReportProjections.RevenueGroupAgg> sumRevenueByCropPlot(@Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("farmId") Integer farmId,
            @Param("plotId") Integer plotId,
            @Param("cropId") Integer cropId,
            @Param("varietyId") Integer varietyId);
}
