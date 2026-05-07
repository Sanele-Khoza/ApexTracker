package com.apextracker.sleep;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.apextracker.config.CurrentUserService;
import com.apextracker.user.User;

@RestController
@RequestMapping("/api/sleep")
public class SleepController {

    private final SleepService sleepService;
    private final SleepRecordRepository sleepRepository;
    private final CurrentUserService currentUser;

    public SleepController(SleepService sleepService, SleepRecordRepository sleepRepository,
            CurrentUserService currentUser) {
        this.sleepService = sleepService;
        this.sleepRepository = sleepRepository;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<SleepResponse> list() {
        User user = currentUser.get();
        return sleepRepository.findByUserOrderByDateDesc(user).stream()
                .map(s -> SleepResponse.from(s, user.getBedTimeTarget(), user.getWakeTimeTarget()))
                .toList();
    }

    @PostMapping
    public SleepResponse create(@RequestBody SleepRequest req) {
        User user = currentUser.get();
        return SleepResponse.from(sleepService.create(user, req), user.getBedTimeTarget(), user.getWakeTimeTarget());
    }

    @PutMapping("/{id}")
    public SleepResponse update(@PathVariable Long id, @RequestBody SleepRequest req) {
        User user = currentUser.get();
        return SleepResponse.from(sleepService.update(id, user, req), user.getBedTimeTarget(), user.getWakeTimeTarget());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        sleepService.delete(id, currentUser.get());
    }
}