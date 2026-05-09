package com.apextracker.task;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apextracker.common.ApiException;
import com.apextracker.common.DateUtils;
import com.apextracker.reminder.Reminder;
import com.apextracker.reminder.ReminderService;
import com.apextracker.user.User;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ReminderService reminderService;

    public TaskService(TaskRepository taskRepository, ReminderService reminderService) {
        this.taskRepository = taskRepository;
        this.reminderService = reminderService;
    }

    public List<Task> listAll(User user) {
        return taskRepository.findByUserOrderByDueDateAscDueTimeAsc(user);
    }

    public List<Task> listByStatus(User user, Task.Status status) {
        return taskRepository.findByUserAndStatus(user, status);
    }

    public Task get(Long id, User user) {
        return taskRepository.findById(id)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> ApiException.notFound("Task not found"));
    }

    @Transactional
    public Task create(User user, TaskCreate req) {
        Task task = new Task();
        task.setUser(user);
        task.setName(req.name().trim());
        task.setDescription(req.description());
        task.setCategory(req.category());
        task.setPriority(req.priority());
        task.setRecurrence(req.recurrence());
        task.setReminderMinutes(req.reminderMinutes());
        task.setEstimatedMinutes(req.estimatedMinutes());
        if (req.dueDate() != null) {
            task.setDueDate(LocalDate.parse(req.dueDate()));
        }
        if (req.dueTime() != null) {
            task.setDueTime(DateUtils.parseTime(req.dueTime()));
        }
        return taskRepository.save(task);
    }

    @Transactional
    public Task update(Long id, User user, TaskCreate req) {
        Task task = get(id, user);
        task.setName(req.name() != null ? req.name().trim() : task.getName());
        if (req.description() != null)
            task.setDescription(req.description());
        if (req.category() != null)
            task.setCategory(req.category());
        if (req.priority() != null)
            task.setPriority(req.priority());
        if (req.recurrence() != null)
            task.setRecurrence(req.recurrence());
        if (req.reminderMinutes() != null)
            task.setReminderMinutes(req.reminderMinutes());
        if (req.estimatedMinutes() != null)
            task.setEstimatedMinutes(req.estimatedMinutes());
        if (req.dueDate() != null) {
            task.setDueDate(LocalDate.parse(req.dueDate()));
            task.setReminderSent(false);
        } else if (req.dueDate() == null && req.name() == null) {
            // no change
        }
        if (req.dueTime() != null) {
            task.setDueTime(DateUtils.parseTime(req.dueTime()));
            task.setReminderSent(false);
        }
        task.setStatus(Task.Status.NOT_STARTED);
        return taskRepository.save(task);
    }

    @Transactional
    public void delete(Long id, User user) {
        Task task = get(id, user);
        taskRepository.delete(task);
    }

    @Transactional
    public Task complete(Long id, User user) {
        Task task = get(id, user);
        if (task.getStatus() == Task.Status.COMPLETED || task.getStatus() == Task.Status.SKIPPED) {
            throw ApiException.badRequest("Task is already completed or skipped");
        }
        task.setStatus(Task.Status.COMPLETED);
        task.setCompletedAt(Instant.now());
        if (task.getRecurrence() != Task.Recurrence.NONE && task.getDueDate() != null) {
            scheduleNextOccurrence(task, user);
        }
        return taskRepository.save(task);
    }

    @Transactional
    public Task start(Long id, User user) {
        Task task = get(id, user);
        task.setStatus(Task.Status.IN_PROGRESS);
        return taskRepository.save(task);
    }

    @Transactional
    public Task skip(Long id, User user) {
        Task task = get(id, user);
        task.setStatus(Task.Status.SKIPPED);
        return taskRepository.save(task);
    }

    @Transactional
    public Task reschedule(Long id, User user, String newDate, String newTime) {
        Task task = get(id, user);
        task.setRescheduleCount(task.getRescheduleCount() + 1);
        task.setStatus(Task.Status.NOT_STARTED);
        if (newDate != null) {
            task.setDueDate(LocalDate.parse(newDate));
        }
        if (newTime != null) {
            task.setDueTime(DateUtils.parseTime(newTime));
        }
        task.setReminderSent(false);
        return taskRepository.save(task);
    }

    @Transactional
    public void generateReminders(User user) {
        if (!user.isReminderEnabled())
            return;
        ZoneId zone = DateUtils.zone(user);
        Instant now = Instant.now();
        LocalDate today = DateUtils.today(user);

        // 1) Upcoming task reminders
        taskRepository.findByUserAndStatusIn(user, List.of(Task.Status.NOT_STARTED, Task.Status.IN_PROGRESS))
                .forEach(task -> {
                    if (task.getDueDate() == null || task.getReminderMinutes() == null || task.isReminderSent())
                        return;
                    Instant dueInstant = task.getDueTime() != null
                            ? DateUtils.atTime(task.getDueDate(), task.getDueTime(), zone)
                            : DateUtils.dayEnd(task.getDueDate(), zone);
                    Instant remindAt = dueInstant.minusSeconds(task.getReminderMinutes() * 60L);
                    if (!now.isBefore(remindAt) && now.isBefore(dueInstant)
                            && !isInQuietHours(user, now)) {
                        String msg = task.getName().toLowerCase().startsWith("study")
                                ? "Your " + task.getName() + " session starts in " + task.getReminderMinutes()
                                        + " minutes."
                                : "Upcoming: " + task.getName() + " due at "
                                        + (task.getDueTime() != null ? task.getDueTime() : "end of day") + ".";
                        reminderService.create(user, Reminder.Type.TASK, msg, now, task.getId(), "TASK");
                        task.setReminderSent(true);
                        taskRepository.save(task);
                    }
                });
    }

    private void scheduleNextOccurrence(Task original, User user) {
        LocalDate nextDate = switch (original.getRecurrence()) {
            case DAILY -> original.getDueDate().plusDays(1);
            case WEEKLY -> original.getDueDate().plusWeeks(1);
            case MONTHLY -> original.getDueDate().plusMonths(1);
            default -> null;
        };
        if (nextDate == null)
            return;
        Task next = new Task();
        next.setUser(user);
        next.setName(original.getName());
        next.setDescription(original.getDescription());
        next.setCategory(original.getCategory());
        next.setPriority(original.getPriority());
        next.setDueDate(nextDate);
        next.setDueTime(original.getDueTime());
        next.setEstimatedMinutes(original.getEstimatedMinutes());
        next.setRecurrence(original.getRecurrence());
        next.setReminderMinutes(original.getReminderMinutes());
        taskRepository.save(next);
    }

    private boolean isInQuietHours(User user, Instant instant) {
        ZoneId zone = DateUtils.zone(user);
        LocalTime now = instant.atZone(zone).toLocalTime();
        LocalTime start = DateUtils.parseTime(user.getQuietHoursStart());
        LocalTime end = DateUtils.parseTime(user.getQuietHoursEnd());
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        } else {
            return !now.isBefore(start) || now.isBefore(end);
        }
    }

    @Transactional
    public void generateDailyTaskOverdue(User user, List<Task> tasks) {
        tasks.forEach(task -> {
            if (task.getStatus() == Task.Status.NOT_STARTED || task.getStatus() == Task.Status.IN_PROGRESS) {
                task.setStatus(Task.Status.OVERDUE);
                task.setMissed(true);
                if (task.getDueTime() != null) {
                    ZoneId zone = DateUtils.zone(user);
                    Instant due = DateUtils.atTime(task.getDueDate(), task.getDueTime(), zone);
                    reminderService.create(user, Reminder.Type.OVERDUE,
                            "Missed: " + task.getName() + " was due at " + task.getDueTime() + ".",
                            Instant.now(), task.getId(), "TASK");
                }
                taskRepository.save(task);
            }
        });
    }
}