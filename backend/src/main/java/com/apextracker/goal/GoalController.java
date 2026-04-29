package com.apextracker.goal;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.apextracker.config.CurrentUserService;
import com.apextracker.user.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;
    private final CurrentUserService currentUser;

    public GoalController(GoalService goalService, CurrentUserService currentUser) {
        this.goalService = goalService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<GoalResponse> list() {
        return goalService.list(currentUser.get());
    }

    @PostMapping
    public GoalResponse create(@Valid @RequestBody GoalRequest req) {
        User user = currentUser.get();
        return goalService.getResponse(user, goalService.create(user, req));
    }

    @PutMapping("/{id}")
    public GoalResponse update(@PathVariable Long id, @RequestBody GoalRequest req) {
        User user = currentUser.get();
        return goalService.getResponse(user, goalService.update(id, user, req));
    }

    @PostMapping("/{id}/progress")
    public GoalResponse setManualProgress(@PathVariable Long id, @RequestBody ProgressUpdate req) {
        User user = currentUser.get();
        return goalService.getResponse(user, goalService.setManualProgress(id, user, req.value()));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        goalService.delete(id, currentUser.get());
    }

    public record ProgressUpdate(double value) {
    }
}