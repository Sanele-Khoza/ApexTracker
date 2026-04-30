package com.apextracker.habit;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.apextracker.config.CurrentUserService;
import com.apextracker.user.User;

@RestController
@RequestMapping("/api/habits")
public class HabitController {

    private final HabitService habitService;
    private final CurrentUserService currentUser;

    public HabitController(HabitService habitService, CurrentUserService currentUser) {
        this.habitService = habitService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<HabitResponse> list() {
        return habitService.list(currentUser.get());
    }

    @PostMapping
    public HabitResponse create(@RequestBody HabitRequest req) {
        User user = currentUser.get();
        return habitService.getResponse(user, habitService.create(user, req));
    }

    @PutMapping("/{id}")
    public HabitResponse update(@PathVariable Long id, @RequestBody HabitRequest req) {
        User user = currentUser.get();
        return habitService.getResponse(user, habitService.update(id, user, req));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        habitService.delete(id, currentUser.get());
    }

    @PostMapping("/{id}/toggle")
    public HabitResponse toggle(@PathVariable Long id, @RequestParam(required = false) String date) {
        User user = currentUser.get();
        return habitService.toggle(id, user, date);
    }

    @PostMapping("/{id}/complete")
    public HabitResponse completeToday(@PathVariable Long id) {
        User user = currentUser.get();
        return habitService.toggle(id, user, com.apextracker.common.DateUtils.today(user).toString());
    }
}