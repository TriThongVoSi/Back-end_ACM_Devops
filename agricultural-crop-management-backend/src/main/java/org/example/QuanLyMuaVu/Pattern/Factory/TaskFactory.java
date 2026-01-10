package org.example.QuanLyMuaVu.Pattern.Factory;

import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Request.CreateTaskRequest;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Entity.Task;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.Pattern.Strategy.StatusTransitionStrategy;
import org.springframework.stereotype.Component;

/**
 * Factory Method Pattern: Task Factory.
 * <p>
 * Creates Task entities with proper defaults.
 * Uses Strategy pattern for initial status.
 */
@Component
@RequiredArgsConstructor
public class TaskFactory implements EntityFactory<Task, CreateTaskRequest> {

    private final StatusTransitionStrategy<TaskStatus> statusStrategy;

    @Override
    public Task create(CreateTaskRequest request, User creator) {
        Task task = new Task();

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(statusStrategy.getInitialStatus());

        // Set dates
        task.setPlannedDate(request.getPlannedDate());
        task.setDueDate(request.getDueDate());

        // Set the user who created/owns this task
        task.setUser(creator);

        return task;
    }

    /**
     * Creates a Task linked to a Season.
     */
    public Task createWithSeason(CreateTaskRequest request, Season season, User creator) {
        Task task = create(request, creator);
        task.setSeason(season);
        return task;
    }
}
