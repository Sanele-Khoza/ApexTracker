package com.apextracker.activity;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.apextracker.common.DateUtils;
import com.apextracker.config.CurrentUserService;
import com.apextracker.user.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;
    private final ActivityRepository activityRepository;
    private final CurrentUserService currentUser;

    public ActivityController(ActivityService activityService, ActivityRepository activityRepository,
            CurrentUserService currentUser) {
        this.activityService = activityService;
        this.activityRepository = activityRepository;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<ActivityResponse> list() {
        User user = currentUser.get();
        return activityRepository.findByUserOrderByStartTimeDesc(user).stream()
                .map(a -> ActivityResponse.from(a, DateUtils.zone(user)))
                .toList();
    }

    @PostMapping
    public ActivityResponse start(@Valid @RequestBody ActivityRequest req) {
        User user = currentUser.get();
        return ActivityResponse.from(activityService.start(user, req), DateUtils.zone(user));
    }

    @PostMapping("/stop")
    public ActivityResponse stopActive() {
        User user = currentUser.get();
        return ActivityResponse.from(activityService.stopActive(user), DateUtils.zone(user));
    }

    @PostMapping("/{id}/stop")
    public ActivityResponse stop(@PathVariable Long id) {
        User user = currentUser.get();
        return ActivityResponse.from(activityService.stop(id, user), DateUtils.zone(user));
    }

    @PutMapping("/{id}")
    public ActivityResponse update(@PathVariable Long id, @RequestBody ActivityRequest req) {
        User user = currentUser.get();
        return ActivityResponse.from(activityService.update(id, user, req), DateUtils.zone(user));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        activityService.delete(id, currentUser.get());
    }
}