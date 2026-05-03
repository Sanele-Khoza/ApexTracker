package com.apextracker.reminder;

import java.time.Instant;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apextracker.config.CurrentUserService;
import com.apextracker.user.User;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderService reminderService;
    private final ReminderRepository reminderRepository;
    private final CurrentUserService currentUser;

    public ReminderController(ReminderService reminderService, ReminderRepository reminderRepository,
            CurrentUserService currentUser) {
        this.reminderService = reminderService;
        this.reminderRepository = reminderRepository;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<ReminderResponse> list() {
        User user = currentUser.get();
        Instant now = Instant.now();
        return reminderRepository.findByUserAndDismissedFalseOrderByScheduledAtDesc(user).stream()
                .limit(50)
                .map(ReminderResponse::from)
                .toList();
    }

    @PostMapping("/{id}/read")
    public ReminderResponse markRead(@PathVariable Long id) {
        return ReminderResponse.from(reminderService.markRead(id, currentUser.get()));
    }

    @PostMapping("/{id}/dismiss")
    public void dismiss(@PathVariable Long id) {
        reminderService.dismiss(id, currentUser.get());
    }
}