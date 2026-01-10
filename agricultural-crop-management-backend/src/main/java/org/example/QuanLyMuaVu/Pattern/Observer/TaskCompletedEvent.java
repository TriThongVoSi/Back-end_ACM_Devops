package org.example.QuanLyMuaVu.Pattern.Observer;

import lombok.Getter;
import org.example.QuanLyMuaVu.Entity.Task;
import org.example.QuanLyMuaVu.Enums.TaskStatus;

/**
 * Observer Pattern: Task Completed Event.
 * <p>
 * Published when a task transitions to DONE status.
 * Listeners can use this to:
 * - Update season progress tracking
 * - Trigger follow-up tasks
 * - Send completion notifications
 * - Log field activity
 */
@Getter
public class TaskCompletedEvent extends DomainEvent {

    private final Integer taskId;
    private final String taskTitle;
    private final Integer seasonId;
    private final TaskStatus previousStatus;

    public TaskCompletedEvent(Task task, TaskStatus previousStatus) {
        super("Task", task.getId() != null ? task.getId().toString() : "unknown");
        this.taskId = task.getId();
        this.taskTitle = task.getTitle();
        this.seasonId = task.getSeason() != null ? task.getSeason().getId() : null;
        this.previousStatus = previousStatus;
    }

    @Override
    public String getEventType() {
        return "TASK_COMPLETED";
    }
}
