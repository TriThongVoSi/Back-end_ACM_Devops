package org.example.QuanLyMuaVu.Repository;

import org.example.QuanLyMuaVu.DTO.Response.AdminReportProjections;
import org.example.QuanLyMuaVu.Entity.Expense;
import org.example.QuanLyMuaVu.Enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Integer>, JpaSpecificationExecutor<Expense> {

        List<Expense> findByItemNameContainingIgnoreCase(String itemName);

        List<Expense> findAllBySeason_Id(Integer seasonId);

        List<Expense> findAllBySeason_IdAndDeletedAtIsNull(Integer seasonId);

        List<Expense> findAllBySeason_IdAndExpenseDateBetween(Integer seasonId, LocalDate from, LocalDate to);

        boolean existsBySeason_Id(Integer seasonId);

        boolean existsBySeason_IdAndDeletedAtIsNull(Integer seasonId);

        // Methods for fetching all farmer's expenses
        List<Expense> findAllByUser_IdOrderByExpenseDateDesc(Long userId);

        List<Expense> findAllByUser_IdAndDeletedAtIsNullOrderByExpenseDateDesc(Long userId);

        List<Expense> findAllByUser_IdAndSeason_IdOrderByExpenseDateDesc(Long userId, Integer seasonId);

        List<Expense> findAllByUser_IdAndItemNameContainingIgnoreCaseOrderByExpenseDateDesc(Long userId,
                        String itemName);

        List<Expense> findAllByUser_IdAndItemNameContainingIgnoreCaseAndDeletedAtIsNullOrderByExpenseDateDesc(
                        Long userId,
                        String itemName);

        Page<Expense> findByUser_IdAndItemNameContainingIgnoreCaseAndDeletedAtIsNull(Long userId,
                        String itemName,
                        Pageable pageable);

        /**
         * Sum total expenses for a season.
         * Used for dashboard expense totals.
         */
        @Query("SELECT COALESCE(SUM(COALESCE(e.amount, e.totalCost)), 0) FROM Expense e " +
                        "WHERE e.season.id = :seasonId AND e.deletedAt IS NULL")
        BigDecimal sumTotalCostBySeasonId(@Param("seasonId") Integer seasonId);

        @Query("SELECT COALESCE(SUM(COALESCE(e.amount, e.totalCost)), 0) FROM Expense e " +
                        "WHERE e.season.id = :seasonId AND e.deletedAt IS NULL AND e.paymentStatus = :status")
        BigDecimal sumEffectiveAmountBySeasonIdAndStatus(
                        @Param("seasonId") Integer seasonId,
                        @Param("status") PaymentStatus status);

        @Query("SELECT COUNT(e) FROM Expense e WHERE e.season.id = :seasonId AND e.deletedAt IS NULL")
        long countBySeasonId(@Param("seasonId") Integer seasonId);

        @Query("SELECT COUNT(e) FROM Expense e WHERE e.season.id = :seasonId AND e.deletedAt IS NULL " +
                        "AND e.attachmentUrl IS NOT NULL")
        long countBySeasonIdWithAttachment(@Param("seasonId") Integer seasonId);

        // ═══════════════════════════════════════════════════════════════
        // ADMIN AGGREGATION METHODS
        // ═══════════════════════════════════════════════════════════════

        /**
         * Sum expenses for date range (admin dashboard).
         */
        @Query("SELECT COALESCE(SUM(COALESCE(e.amount, e.totalCost)), 0) FROM Expense e " +
                        "WHERE e.deletedAt IS NULL AND e.expenseDate BETWEEN :startDate AND :endDate")
        BigDecimal sumAmountByExpenseDateBetween(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Sum expenses using report filters.
         */
        @Query("SELECT COALESCE(SUM(COALESCE(e.amount, e.totalCost)), 0) FROM Expense e " +
                        "WHERE e.deletedAt IS NULL " +
                        "AND (:fromDate IS NULL OR e.expenseDate >= :fromDate) " +
                        "AND (:toDate IS NULL OR e.expenseDate <= :toDate) " +
                        "AND (:farmId IS NULL OR e.season.plot.farm.id = :farmId) " +
                        "AND (:plotId IS NULL OR e.season.plot.id = :plotId) " +
                        "AND (:cropId IS NULL OR e.season.crop.id = :cropId) " +
                        "AND (:varietyId IS NULL OR e.season.variety.id = :varietyId)")
        BigDecimal sumAmountByFilters(@Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("farmId") Integer farmId,
                        @Param("plotId") Integer plotId,
                        @Param("cropId") Integer cropId,
                        @Param("varietyId") Integer varietyId);

        /**
         * Count expenses using report filters.
         */
        @Query("SELECT COUNT(e) FROM Expense e " +
                        "WHERE e.deletedAt IS NULL " +
                        "AND (:fromDate IS NULL OR e.expenseDate >= :fromDate) " +
                        "AND (:toDate IS NULL OR e.expenseDate <= :toDate) " +
                        "AND (:farmId IS NULL OR e.season.plot.farm.id = :farmId) " +
                        "AND (:plotId IS NULL OR e.season.plot.id = :plotId) " +
                        "AND (:cropId IS NULL OR e.season.crop.id = :cropId) " +
                        "AND (:varietyId IS NULL OR e.season.variety.id = :varietyId)")
        long countByFilters(@Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("farmId") Integer farmId,
                        @Param("plotId") Integer plotId,
                        @Param("cropId") Integer cropId,
                        @Param("varietyId") Integer varietyId);

        /**
         * Sum expenses grouped by season ID.
         */
        @Query("SELECT e.season.id AS seasonId, COALESCE(SUM(COALESCE(e.amount, e.totalCost)), 0) AS totalExpense " +
                        "FROM Expense e WHERE e.deletedAt IS NULL AND e.season.id IN :seasonIds GROUP BY e.season.id")
        List<AdminReportProjections.SeasonExpenseAgg> sumExpensesBySeasonIds(
                        @Param("seasonIds") Set<Integer> seasonIds);

        /**
         * Sum expenses by category using report filters.
         */
        @Query("SELECT e.category AS category, " +
                        "COALESCE(SUM(COALESCE(e.amount, e.totalCost)), 0) AS totalCost, " +
                        "COUNT(e.id) AS expenseCount " +
                        "FROM Expense e " +
                        "WHERE e.deletedAt IS NULL " +
                        "AND (:fromDate IS NULL OR e.expenseDate >= :fromDate) " +
                        "AND (:toDate IS NULL OR e.expenseDate <= :toDate) " +
                        "AND (:farmId IS NULL OR e.season.plot.farm.id = :farmId) " +
                        "AND (:plotId IS NULL OR e.season.plot.id = :plotId) " +
                        "AND (:cropId IS NULL OR e.season.crop.id = :cropId) " +
                        "AND (:varietyId IS NULL OR e.season.variety.id = :varietyId) " +
                        "GROUP BY e.category")
        List<AdminReportProjections.CostCategoryAgg> sumCostByCategory(@Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("farmId") Integer farmId,
                        @Param("plotId") Integer plotId,
                        @Param("cropId") Integer cropId,
                        @Param("varietyId") Integer varietyId);

        /**
         * Sum expenses by vendor using report filters.
         */
        @Query("SELECT s.id AS vendorId, s.name AS vendorName, " +
                        "COALESCE(SUM(COALESCE(e.amount, e.totalCost)), 0) AS totalCost, " +
                        "COUNT(e.id) AS expenseCount " +
                        "FROM Expense e LEFT JOIN e.supplier s " +
                        "WHERE e.deletedAt IS NULL " +
                        "AND (:fromDate IS NULL OR e.expenseDate >= :fromDate) " +
                        "AND (:toDate IS NULL OR e.expenseDate <= :toDate) " +
                        "AND (:farmId IS NULL OR e.season.plot.farm.id = :farmId) " +
                        "AND (:plotId IS NULL OR e.season.plot.id = :plotId) " +
                        "AND (:cropId IS NULL OR e.season.crop.id = :cropId) " +
                        "AND (:varietyId IS NULL OR e.season.variety.id = :varietyId) " +
                        "GROUP BY s.id, s.name")
        List<AdminReportProjections.CostVendorAgg> sumCostByVendor(@Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("farmId") Integer farmId,
                        @Param("plotId") Integer plotId,
                        @Param("cropId") Integer cropId,
                        @Param("varietyId") Integer varietyId);

        /**
         * Sum expenses by day using report filters.
         */
        @Query("SELECT e.expenseDate AS periodStart, " +
                        "COALESCE(SUM(COALESCE(e.amount, e.totalCost)), 0) AS totalCost " +
                        "FROM Expense e " +
                        "WHERE e.deletedAt IS NULL " +
                        "AND (:fromDate IS NULL OR e.expenseDate >= :fromDate) " +
                        "AND (:toDate IS NULL OR e.expenseDate <= :toDate) " +
                        "AND (:farmId IS NULL OR e.season.plot.farm.id = :farmId) " +
                        "AND (:plotId IS NULL OR e.season.plot.id = :plotId) " +
                        "AND (:cropId IS NULL OR e.season.crop.id = :cropId) " +
                        "AND (:varietyId IS NULL OR e.season.variety.id = :varietyId) " +
                        "GROUP BY e.expenseDate " +
                        "ORDER BY e.expenseDate")
        List<AdminReportProjections.CostTimeAgg> sumCostByDay(@Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("farmId") Integer farmId,
                        @Param("plotId") Integer plotId,
                        @Param("cropId") Integer cropId,
                        @Param("varietyId") Integer varietyId);

        /**
         * Sum expenses by crop and plot using report filters.
         */
        @Query("SELECT s.crop.id AS cropId, s.crop.cropName AS cropName, " +
                        "s.plot.id AS plotId, s.plot.plotName AS plotName, " +
                        "COALESCE(SUM(COALESCE(e.amount, e.totalCost)), 0) AS totalCost " +
                        "FROM Expense e JOIN e.season s " +
                        "WHERE e.deletedAt IS NULL " +
                        "AND (:fromDate IS NULL OR e.expenseDate >= :fromDate) " +
                        "AND (:toDate IS NULL OR e.expenseDate <= :toDate) " +
                        "AND (:farmId IS NULL OR s.plot.farm.id = :farmId) " +
                        "AND (:plotId IS NULL OR s.plot.id = :plotId) " +
                        "AND (:cropId IS NULL OR s.crop.id = :cropId) " +
                        "AND (:varietyId IS NULL OR s.variety.id = :varietyId) " +
                        "GROUP BY s.crop.id, s.crop.cropName, s.plot.id, s.plot.plotName")
        List<AdminReportProjections.CostCropPlotAgg> sumCostByCropPlot(@Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("farmId") Integer farmId,
                        @Param("plotId") Integer plotId,
                        @Param("cropId") Integer cropId,
                        @Param("varietyId") Integer varietyId);
}
