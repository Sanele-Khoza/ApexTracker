package com.apextracker.task;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.apextracker.config.CurrentUserService;
import com.apextracker.user.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final CurrentUserService currentUser;

    public TaskController(TaskService taskService, TaskRepository taskRepository, CurrentUserService currentUser) {
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<TaskResponse> list(@RequestParam(required = false) Task.Status status,
            @RequestParam(required = false) Task.Category category,
            @RequestParam(required = false) String search) {
        User user = currentUser.get();
        List<Task> tasks = taskService.listAll(user);
        if (status != null) {
            tasks = tasks.stream().filter(t -> t.getStatus() == status).toList();
        }
        if (category != null) {
            tasks = tasks.stream().filter(t -> t.getCategory() == category).toList();
        }
        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            tasks = tasks.stream()
                    .filter(t -> t.getName().toLowerCase().contains(q)
                            || (t.getDescription() != null && t.getDescription().toLowerCase().contains(q)))
                    .toList();
        }
        return tasks.stream().map(TaskResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id) {
        return TaskResponse.from(taskService.get(id, currentUser.get()));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskCreate req) {
        return ResponseEntity.ok(TaskResponse.from(taskService.create(currentUser.get(), req)));
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @RequestBody TaskCreate req) {
        return TaskResponse.from(taskService.update(id, currentUser.get(), req));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        taskService.delete(id, currentUser.get());
    }

    @PostMapping("/{id}/complete")
    public TaskResponse complete(@PathVariable Long id) {
        return TaskResponse.from(taskService.complete(id, currentUser.get()));
    }

    @PostMapping("/{id}/start")
    public TaskResponse start(@PathVariable Long id) {
        return TaskResponse.from(taskService.start(id, currentUser.get()));
    }

    @PostMapping("/{id}/skip")
    public TaskResponse skip(@PathVariable Long id) {
        return TaskResponse.from(taskService.skip(id, currentUser.get()));
    }

    @PostMapping("/{id}/reschedule")
    public TaskResponse reschedule(@PathVariable Long id, @RequestBody RescheduleRequest req) {
        return TaskResponse.from(taskService.reschedule(id, currentUser.get(), req.newDate(), req.newTime()));
    }

    public record RescheduleRequest(String newDate, String newTime) {
    }
}