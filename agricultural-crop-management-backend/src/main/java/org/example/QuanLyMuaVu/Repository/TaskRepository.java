package org.example.QuanLyMuaVu.Repository;

import org.example.QuanLyMuaVu.Entity.Task;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Integer>,
                org.springframework.data.jpa.repository.JpaSpecificationExecutor<Task> {
        // Existing season-scoped methods
        List<Task> findByTitleContainingIgnoreCase(String title);

        List<Task> findAllBySeason_Id(Integer seasonId);

        boolean existsBySeason_Id(Integer seasonId);

        long countBySeason_IdAndStatusIn(Integer seasonId, List<TaskStatus> statuses);

        long countBySeason_IdAndStatusNot(Integer seasonId, TaskStatus status);

        List<Task> findBySeason_IdAndStatusNot(Integer seasonId, TaskStatus status);

        // User-scoped methods for Tasks Workspace
        Page<Task> findByUser(User user, Pageable pageable);

        Page<Task> findByUserAndStatus(User user, TaskStatus status, Pageable pageable);

        Page<Task> findByUserAndSeasonId(User user, Integer seasonId, Pageable pageable);

        Page<Task> findByUserAndTitleContainingIgnoreCase(User user, String title, Pageable pageable);

        Optional<Task> findByIdAndUser(Integer id, User user);

        // Complex query for multiple filters
        @Query("SELECT t FROM Task t WHERE t.user = :user " +
                        "AND (:status IS NULL OR t.status = :status) " +
                        "AND (:seasonId IS NULL OR t.season.id = :seasonId) " +
                        "AND (:searchQuery IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :searchQuery, '%')))")
        Page<Task> findByUserWithFilters(
                        @Param("user") User user,
                        @Param("status") TaskStatus status,
                        @Param("seasonId") Integer seasonId,
                        @Param("searchQuery") String searchQuery,
                        Pageable pageable);

        // Bulk update for overdue tasks
        @Modifying
        @Query("UPDATE Task t SET t.status = :overdueStatus " +
                        "WHERE t.dueDate < :currentDate " +
                        "AND t.status IN :pendingStatuses")
        int updateOverdueTasks(
                        @Param("currentDate") LocalDate currentDate,
                        @Param("overdueStatus") TaskStatus overdueStatus,
                        @Param("pendingStatuses") List<TaskStatus> pendingStatuses);

        // ===========================================================================
        // DASHBOARD QUERIES
        // ===========================================================================

        /**
         * Find today's tasks for dashboard table.
         * Returns tasks where dueDate = today or plannedDate = today
         */
        @Query("SELECT t FROM Task t WHERE t.user = :user AND (t.dueDate = :today OR t.plannedDate = :today) ORDER BY t.dueDate ASC")
        Page<Task> findTodayTasksByUser(@Param("user") User user, @Param("today") LocalDate today, Pageable pageable);

        /**
         * Find upcoming tasks due within N days.
         */
        @Query("SELECT t FROM Task t WHERE t.user = :user " +
                        "AND t.dueDate > :today AND t.dueDate <= :untilDate " +
                        "AND t.status NOT IN :excludedStatuses " +
                        "ORDER BY t.dueDate ASC")
        List<Task> findUpcomingTasksByUser(
                        @Param("user") User user,
                        @Param("today") LocalDate today,
                        @Param("untilDate") LocalDate untilDate,
                        @Param("excludedStatuses") List<TaskStatus> excludedStatuses);

        /**
         * Count overdue tasks for a user.
         */
        @Query("SELECT COUNT(t) FROM Task t WHERE t.user = :user " +
                        "AND t.dueDate < :today " +
                        "AND t.status NOT IN :completedStatuses")
        long countOverdueByUser(@Param("user") User user, @Param("today") LocalDate today,
                        @Param("completedStatuses") List<TaskStatus> completedStatuses);

        /**
         * Count tasks completed on time for a season (for on-time percentage).
         * A task is on-time if actualEndDate <= dueDate
         */
        @Query("SELECT COUNT(t) FROM Task t WHERE t.season.id = :seasonId " +
                        "AND t.status = 'DONE' " +
                        "AND t.actualEndDate IS NOT NULL " +
                        "AND t.actualEndDate <= t.dueDate")
        long countCompletedOnTimeBySeasonId(@Param("seasonId") Integer seasonId);

        /**
         * Count total completed tasks for a season.
         */
        @Query("SELECT COUNT(t) FROM Task t WHERE t.season.id = :seasonId AND t.status = 'DONE'")
        long countCompletedBySeasonId(@Param("seasonId") Integer seasonId);

        // ═══════════════════════════════════════════════════════════════════════════
        // BR176/BR180: Expense-Task Validation Queries
        // ═══════════════════════════════════════════════════════════════════════════

        /**
         * BR176/BR180: Check if a task exists and belongs to the specified season.
         * Used to validate [Expense.task_id] belongs to the selected Season.
         *
         * @param taskId   the task ID to validate
         * @param seasonId the season ID the task should belong to
         * @return true if task exists and belongs to the season
         */
        boolean existsByIdAndSeasonId(Integer taskId, Integer seasonId);

        /**
         * BR176/BR180: Find task by ID and season for validation.
         *
         * @param taskId   the task ID
         * @param seasonId the season ID
         * @return Optional containing the task if found
         */
        Optional<Task> findByIdAndSeasonId(Integer taskId, Integer seasonId);
}
