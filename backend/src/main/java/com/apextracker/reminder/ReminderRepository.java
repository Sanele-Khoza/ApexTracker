package com.apextracker.reminder;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apextracker.user.User;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByUserAndDismissedFalseOrderByScheduledAtDesc(User user);

    List<Reminder> findByUserAndDismissedFalseOrderByScheduledAtAsc(User user);

    boolean existsByUserAndTypeAndScheduledAtBetween(User user, Reminder.Type type, Instant start, Instant end);

    List<Reminder> findByDismissedFalseAndScheduledAtLessThanEqual(Instant now);

    void deleteByUser(User user);
}