package org.example.QuanLyMuaVu.Repository;

import org.example.QuanLyMuaVu.DTO.Response.AdminReportProjections;
import org.example.QuanLyMuaVu.Entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Integer> {

    List<Expense> findByItemNameContainingIgnoreCase(String itemName);

    List<Expense> findAllBySeason_Id(Integer seasonId);

    List<Expense> findAllBySeason_IdAndExpenseDateBetween(Integer seasonId, LocalDate from, LocalDate to);

    boolean existsBySeason_Id(Integer seasonId);

    // Methods for fetching all farmer's expenses
    List<Expense> findAllByUser_IdOrderByExpenseDateDesc(Long userId);

    List<Expense> findAllByUser_IdAndSeason_IdOrderByExpenseDateDesc(Long userId, Integer seasonId);

    List<Expense> findAllByUser_IdAndItemNameContainingIgnoreCaseOrderByExpenseDateDesc(Long userId, String itemName);

    /**
     * Sum total expenses for a season.
     * Used for dashboard expense totals.
     */
    @Query("SELECT COALESCE(SUM(e.totalCost), 0) FROM Expense e WHERE e.season.id = :seasonId")
    BigDecimal sumTotalCostBySeasonId(@Param("seasonId") Integer seasonId);

    // ═══════════════════════════════════════════════════════════════
    // ADMIN AGGREGATION METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Sum expenses for date range (admin dashboard).
     */
    @Query("SELECT COALESCE(SUM(e.totalCost), 0) FROM Expense e WHERE e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByExpenseDateBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Sum expenses grouped by season ID.
     */
    @Query("SELECT e.season.id AS seasonId, COALESCE(SUM(e.totalCost), 0) AS totalExpense " +
            "FROM Expense e WHERE e.season.id IN :seasonIds GROUP BY e.season.id")
    List<AdminReportProjections.SeasonExpenseAgg> sumExpensesBySeasonIds(
            @Param("seasonIds") Set<Integer> seasonIds);
}
