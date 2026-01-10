package org.example.QuanLyMuaVu.Service;

import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Request.TaskRequest;
import org.example.QuanLyMuaVu.DTO.Response.TaskResponse;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Entity.Task;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.Repository.SeasonRepository;
import org.example.QuanLyMuaVu.Repository.TaskRepository;
import org.example.QuanLyMuaVu.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final SeasonRepository seasonRepository;

    public TaskResponse create(TaskRequest request) {
        User user = userRepository.findById(request.getUserId()).orElseThrow();
        Season season = null;
        if (request.getSeasonId() != null) {
            season = seasonRepository.findById(request.getSeasonId()).orElse(null);
        }

        Task task = Task.builder()
                .user(user)
                .season(season)
                .title(request.getTitle())
                .description(request.getDescription())
                .plannedDate(request.getPlannedDate())
                .dueDate(request.getDueDate())
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.PENDING)
                .build();
        task = taskRepository.save(task);
        return toResponse(task);
    }

    public List<TaskResponse> getAll() {
        return taskRepository.findAll().stream().map(this::toResponse).toList();
    }

    public TaskResponse update(Integer id, TaskRequest request) {
        Task task = taskRepository.findById(id).orElseThrow();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPlannedDate(request.getPlannedDate());
        task.setDueDate(request.getDueDate());
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        return toResponse(taskRepository.save(task));
    }

    public void delete(Integer id) {
        taskRepository.deleteById(id);
    }

    private TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .taskId(task.getId())
                .userName(task.getUser() != null ? task.getUser().getUsername() : null)
                .seasonId(task.getSeason() != null ? task.getSeason().getId() : null)
                .seasonName(task.getSeason() != null ? task.getSeason().getSeasonName() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .plannedDate(task.getPlannedDate())
                .dueDate(task.getDueDate())
                .status(task.getStatus() != null ? task.getStatus().getCode() : null)
                .actualStartDate(task.getActualStartDate())
                .actualEndDate(task.getActualEndDate())
                .notes(task.getNotes())
                .userId(task.getUser() != null ? task.getUser().getId() : null)
                .createdAt(task.getCreatedAt())
                .build();
    }
}
