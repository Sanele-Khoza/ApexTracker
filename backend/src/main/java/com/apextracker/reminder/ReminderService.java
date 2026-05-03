package com.apextracker.reminder;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apextracker.common.ApiException;
import com.apextracker.user.User;

@Service
public class ReminderService {

    private final ReminderRepository reminderRepository;

    public ReminderService(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    @Transactional
    public Reminder create(User user, Reminder.Type type, String message, Instant scheduledAt, Long relatedId,
            String entityType) {
        Reminder reminder = new Reminder();
        reminder.setUser(user);
        reminder.setType(type);
        reminder.setMessage(message);
        reminder.setScheduledAt(scheduledAt != null ? scheduledAt : Instant.now());
        reminder.setRelatedId(relatedId);
        reminder.setEntityType(entityType);
        return reminderRepository.save(reminder);
    }

    @Transactional
    public Reminder markRead(Long id, User user) {
        Reminder reminder = findOwned(id, user);
        reminder.setRead(true);
        return reminderRepository.save(reminder);
    }

    @Transactional
    public void dismiss(Long id, User user) {
        Reminder reminder = findOwned(id, user);
        reminder.setDismissed(true);
        reminderRepository.save(reminder);
    }

    private Reminder findOwned(Long id, User user) {
        return reminderRepository.findById(id)
                .filter(r -> r.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> ApiException.notFound("Reminder not found"));
    }
}