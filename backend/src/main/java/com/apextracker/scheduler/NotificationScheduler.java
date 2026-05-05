package com.apextracker.scheduler;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.apextracker.common.DateUtils;
import com.apextracker.goal.Goal;
import com.apextracker.goal.GoalRepository;
import com.apextracker.habit.Habit;
import com.apextracker.habit.HabitCompletionRepository;
import com.apextracker.habit.HabitRepository;
import com.apextracker.performance.PerformanceService;
import com.apextracker.reminder.Reminder;
import com.apextracker.reminder.ReminderRepository;
import com.apextracker.reminder.ReminderService;
import com.apextracker.review.DailyReviewService;
import com.apextracker.task.Task;
import com.apextracker.task.TaskRepository;
import com.apextracker.task.TaskService;
import com.apextracker.user.User;
import com.apextracker.user.UserRepository;

@Component
public class NotificationScheduler {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final TaskService taskService;
    private final ReminderService reminderService;
    private final ReminderRepository reminderRepository;
    private final HabitRepository habitRepository;
    private final HabitCompletionRepository habitCompletionRepository;
    private final GoalRepository goalRepository;
    private final DailyReviewService dailyReviewService;
    private final PerformanceService performanceService;

    public NotificationScheduler(UserRepository userRepository, TaskRepository taskRepository, TaskService taskService,
            ReminderService reminderService, ReminderRepository reminderRepository, HabitRepository habitRepository,
            HabitCompletionRepository habitCompletionRepository, GoalRepository goalRepository,
            DailyReviewService dailyReviewService, PerformanceService performanceService) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.taskService = taskService;
        this.reminderService = reminderService;
        this.reminderRepository = reminderRepository;
        this.habitRepository = habitRepository;
        this.habitCompletionRepository = habitCompletionRepository;
        this.goalRepository = goalRepository;
        this.dailyReviewService = dailyReviewService;
        this.performanceService = performanceService;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 15000)
    @Transactional
    public void run() {
        for (User user : userRepository.findAll()) {
            try {
                processUser(user);
            } catch (Exception ignored) {
                // Never let one user's failure stop the scheduler
            }
        }
    }

    private void processUser(User user) {
        ZoneId zone = DateUtils.zone(user);
        Instant now = Instant.now();
        LocalDate today = DateUtils.today(user);

        markOverdue(user, zone, now, today);
        if (user.isReminderEnabled() && !inQuietHours(user, zone, now)) {
            taskService.generateReminders(user);
            habitReminders(user, zone, now, today);
            sleepAndWakeReminders(user, zone, now, today);
            goalReminders(user, today);
        }
        dailyRollover(user, today);
    }

    private void markOverdue(User user, ZoneId zone, Instant now, LocalDate today) {
        for (Task task : taskRepository.findOverdueCandidates(user.getId(),
                java.util.List.of(Task.Status.NOT_STARTED, Task.Status.IN_PROGRESS), today)) {
            boolean overdue;
            if (task.getDueTime() != null) {
                Instant due = DateUtils.atTime(task.getDueDate(), task.getDueTime(), zone);
                overdue = now.isAfter(due);
            } else {
                overdue = task.getDueDate().isBefore(today);
            }
            if (overdue) {
                task.setStatus(Task.Status.OVERDUE);
                task.setMissed(true);
                taskRepository.save(task);
                reminderService.create(user, Reminder.Type.OVERDUE,
                        "Missed: " + task.getName() + " was scheduled for "
                                + (task.getDueTime() != null ? task.getDueTime() : task.getDueDate())
                                + ". Would you like to complete or reschedule it?",
                        now, task.getId(), "TASK");
            }
        }
    }

    private void habitReminders(User user, ZoneId zone, Instant now, LocalDate today) {
        LocalTime wake = LocalTime.parse(user.getWakeTimeTarget());
        for (Habit habit : habitRepository.findByUserOrderByCreatedAtAsc(user)) {
            if (habit.getReminderMinutes() == null || today.equals(habit.getReminderSentDate())) {
                continue;
            }
            if (habitCompletionRepository.existsByHabitAndDate(habit, today)) {
                continue;
            }
            LocalTime reminderTime = wake.plusMinutes(habit.getReminderMinutes());
            LocalTime nowTime = now.atZone(zone).toLocalTime();
            if (!nowTime.isBefore(reminderTime)) {
                reminderService.create(user, Reminder.Type.HABIT,
                        "Habit reminder: " + habit.getName() + " is still to be done today.",
                        now, habit.getId(), "HABIT");
                habit.setReminderSentDate(today);
                habitRepository.save(habit);
            }
        }
    }

    private void sleepAndWakeReminders(User user, ZoneId zone, Instant now, LocalDate today) {
        LocalTime nowTime = now.atZone(zone).toLocalTime();
        LocalTime bedTarget = DateUtils.parseTime(user.getBedTimeTarget());
        LocalTime wakeTarget = DateUtils.parseTime(user.getWakeTimeTarget());
        Instant dayStart = DateUtils.dayStart(today, zone);

        if (withinWindow(nowTime, bedTarget) && !reminderRepository.existsByUserAndTypeAndScheduledAtBetween(user,
                Reminder.Type.SLEEP, dayStart, now)) {
            reminderService.create(user, Reminder.Type.SLEEP,
                    "It is close to your target bedtime of " + user.getBedTimeTarget() + ". Consider winding down.",
                    now, null, "SLEEP");
        }
        if (Math.abs(minutesBetween(nowTime, wakeTarget)) <= 5 && !reminderRepository
                .existsByUserAndTypeAndScheduledAtBetween(user, Reminder.Type.WAKEUP, dayStart, now)) {
            reminderService.create(user, Reminder.Type.WAKEUP,
                    "Good morning " + user.getName() + "! Target wake time is " + user.getWakeTimeTarget() + ".",
                    now, null, "SLEEP");
        }
    }

    private void goalReminders(User user, LocalDate today) {
        for (Goal goal : goalRepository.findByUserAndStatus(user, Goal.Status.ACTIVE)) {
            if (goal.isReminderSent()) {
                continue;
            }
            long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, goal.getEndDate());
            if (daysLeft <= 1 && goal.getProgressValue() < goal.getTargetValue()) {
                reminderService.create(user, Reminder.Type.GOAL,
                        "Goal \"" + goal.getName() + "\" ends soon and is at "
                                + Math.round(goal.getProgressValue() * 100.0 / Math.max(1, goal.getTargetValue()))
                                + "%.",
                        Instant.now(), goal.getId(), "GOAL");
                goal.setReminderSent(true);
                goalRepository.save(goal);
            }
        }
    }

    private void dailyRollover(User user, LocalDate today) {
        LocalDate yesterday = today.minusDays(1);
        performanceService.computeForDate(user, yesterday);
        dailyReviewService.getOrGenerate(user, yesterday);
    }

    private boolean inQuietHours(User user, ZoneId zone, Instant now) {
        LocalTime nowTime = now.atZone(zone).toLocalTime();
        LocalTime start = DateUtils.parseTime(user.getQuietHoursStart());
        LocalTime end = DateUtils.parseTime(user.getQuietHoursEnd());
        if (start.isBefore(end)) {
            return !nowTime.isBefore(start) && nowTime.isBefore(end);
        }
        return !nowTime.isBefore(start) || nowTime.isBefore(end);
    }

    private boolean withinWindow(LocalTime now, LocalTime target) {
        return Math.abs(minutesBetween(now, target)) <= 5;
    }

    private int minutesBetween(LocalTime a, LocalTime b) {
        int diff = (a.getHour() * 60 + a.getMinute()) - (b.getHour() * 60 + b.getMinute());
        if (diff > 720)
            diff -= 1440;
        if (diff < -720)
            diff += 1440;
        return diff;
    }
}