package com.apextracker.dashboard;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.apextracker.activity.Activity;
import com.apextracker.activity.ActivityRepository;
import com.apextracker.common.DateUtils;
import com.apextracker.habit.Habit;
import com.apextracker.habit.HabitCompletionRepository;
import com.apextracker.habit.HabitRepository;
import com.apextracker.performance.PerformanceMetric;
import com.apextracker.performance.PerformanceService;
import com.apextracker.reminder.Reminder;
import com.apextracker.reminder.ReminderRepository;
import com.apextracker.sleep.SleepRecord;
import com.apextracker.sleep.SleepRecordRepository;
import com.apextracker.study.StudySessionRepository;
import com.apextracker.task.Task;
import com.apextracker.task.TaskRepository;
import com.apextracker.user.User;

@Service
public class DashboardService {

    private final TaskRepository taskRepository;
    private final StudySessionRepository studyRepository;
    private final SleepRecordRepository sleepRepository;
    private final ActivityRepository activityRepository;
    private final HabitRepository habitRepository;
    private final HabitCompletionRepository habitCompletionRepository;
    private final ReminderRepository reminderRepository;
    private final PerformanceService performanceService;

    public DashboardService(TaskRepository taskRepository, StudySessionRepository studyRepository,
            SleepRecordRepository sleepRepository, ActivityRepository activityRepository,
            HabitRepository habitRepository, HabitCompletionRepository habitCompletionRepository,
            ReminderRepository reminderRepository, PerformanceService performanceService) {
        this.taskRepository = taskRepository;
        this.studyRepository = studyRepository;
        this.sleepRepository = sleepRepository;
        this.activityRepository = activityRepository;
        this.habitRepository = habitRepository;
        this.habitCompletionRepository = habitCompletionRepository;
        this.reminderRepository = reminderRepository;
        this.performanceService = performanceService;
    }

    public Map<String, Object> summary(User user) {
        ZoneId zone = DateUtils.zone(user);
        LocalDate today = DateUtils.today(user);
        ZonedDateTime now = Instant.now().atZone(zone);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("date", today.toString());
        out.put("time", now.toLocalTime().withNano(0).toString());
        out.put("name", user.getName());
        out.put("timezone", user.getTimezone());

        List<Task> todayTasks = taskRepository.findByUserAndDueDate(user, today);
        long completed = todayTasks.stream().filter(t -> t.getStatus() == Task.Status.COMPLETED).count();
        long skipped = todayTasks.stream().filter(t -> t.getStatus() == Task.Status.SKIPPED).count();
        long pending = todayTasks.stream()
                .filter(t -> t.getStatus() == Task.Status.NOT_STARTED || t.getStatus() == Task.Status.IN_PROGRESS).count();
        long overdue = taskRepository.findByUserAndStatus(user, Task.Status.OVERDUE).size();

        Map<String, Object> taskMap = new LinkedHashMap<>();
        taskMap.put("planned", todayTasks.size());
        taskMap.put("completed", completed);
        taskMap.put("pending", pending);
        taskMap.put("overdue", overdue);
        taskMap.put("skipped", skipped);
        out.put("tasks", taskMap);

        List<Map<String, Object>> overdueList = taskRepository
                .findByUserAndStatusIn(user, List.of(Task.Status.OVERDUE)).stream()
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("name", t.getName());
                    m.put("category", t.getCategory().name());
                    m.put("dueDate", t.getDueDate() != null ? t.getDueDate().toString() : null);
                    m.put("dueTime", t.getDueTime() != null ? t.getDueTime().toString() : null);
                    return m;
                }).toList();
        out.put("overdueTasks", overdueList);

        long studyMinutes = Math.round(studyRepository.sumSecondsBetweenDates(user, today, today) / 60.0);
        out.put("studyMinutes", studyMinutes);
        out.put("studyWeeklyMinutes", Math.round(studyRepository.sumSecondsBetweenDates(user,
                today.minusDays(performanceService.weekStartOffset(user, today)), today) / 60.0));
        out.put("studyWeeklyGoalMinutes", user.getWeeklyStudyGoalMinutes());

        SleepRecord lastSleep = sleepRepository.findByUserAndDate(user, today).orElse(null);
        out.put("sleepMinutes", lastSleep != null ? lastSleep.getDurationMinutes() : null);
        out.put("sleepRecorded", lastSleep != null);

        List<Habit> habits = habitRepository.findByUserOrderByCreatedAtAsc(user);
        long dailyHabits = habits.stream().filter(h -> h.getFrequency() == Habit.Frequency.DAILY).count();
        long completedHabits = habitCompletionRepository.countByUserAndDateBetween(user, today, today);
        out.put("habitsCompleted", completedHabits);
        out.put("habitsTotal", dailyHabits);

        PerformanceMetric metric = performanceService.computeToday(user);
        out.put("productivityScore", metric.getProductivityScore());
        out.put("performanceScore", metric.getOverallScore());
        Map<String, Object> scores = new LinkedHashMap<>();
        scores.put("productivity", metric.getProductivityScore());
        scores.put("study", metric.getStudyScore());
        scores.put("routine", metric.getRoutineScore());
        scores.put("sleep", metric.getSleepScore());
        scores.put("activity", metric.getActivityScore());
        scores.put("overall", metric.getOverallScore());
        out.put("scores", scores);
        out.put("scoreDisclaimer", "App-generated productivity indicator, not a medical or scientific measurement.");

        Activity active = activityRepository.findByUserAndActiveTrue(user).stream().findFirst().orElse(null);
        if (active != null) {
            Map<String, Object> activeMap = new LinkedHashMap<>();
            activeMap.put("id", active.getId());
            activeMap.put("name", active.getName());
            activeMap.put("category", active.getCategory().name());
            activeMap.put("startTime", active.getStartTime().toString());
            out.put("activeActivity", activeMap);
        }

        List<Map<String, Object>> reminders = reminderRepository
                .findByUserAndDismissedFalseOrderByScheduledAtAsc(user).stream()
                .limit(8)
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("type", r.getType().name());
                    m.put("message", r.getMessage());
                    m.put("scheduledAt", r.getScheduledAt().toString());
                    m.put("relatedId", r.getRelatedId());
                    return m;
                }).toList();
        out.put("reminders", reminders);

        List<Map<String, Object>> upcoming = todayTasks.stream()
                .filter(t -> t.getStatus() == Task.Status.NOT_STARTED || t.getStatus() == Task.Status.IN_PROGRESS)
                .filter(t -> t.getDueTime() != null)
                .sorted((a, b) -> a.getDueTime().compareTo(b.getDueTime()))
                .limit(5)
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("name", t.getName());
                    m.put("dueTime", t.getDueTime().toString());
                    m.put("category", t.getCategory().name());
                    return m;
                }).toList();
        out.put("upcoming", upcoming);
        return out;
    }

    public Map<String, Object> planVsActual(User user, String dateStr) {
        ZoneId zone = DateUtils.zone(user);
        LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : DateUtils.today(user);
        List<Task> tasks = taskRepository.findByUserAndDueDate(user, date);

        long studyPlanned = tasks.stream().filter(t -> t.getCategory() == Task.Category.STUDY)
                .mapToLong(t -> t.getEstimatedMinutes() == null ? 0 : t.getEstimatedMinutes()).sum();
        long codingPlanned = tasks.stream().filter(t -> t.getCategory() == Task.Category.PROJECT
                || t.getCategory() == Task.Category.WORK)
                .mapToLong(t -> t.getEstimatedMinutes() == null ? 0 : t.getEstimatedMinutes()).sum();
        long exercisePlanned = tasks.stream().filter(t -> t.getCategory() == Task.Category.EXERCISE)
                .mapToLong(t -> t.getEstimatedMinutes() == null ? 0 : t.getEstimatedMinutes()).sum();

        long studyActual = Math.round(studyRepository.sumSecondsBetweenDates(user, date, date) / 60.0);
        long codingActual = activityRepository.sumDurationByCategoryBetween(user, Activity.Category.CODING,
                DateUtils.dayStart(date, zone), DateUtils.dayEnd(date, zone));
        long exerciseActual = activityRepository.sumDurationByCategoryBetween(user, Activity.Category.EXERCISING,
                DateUtils.dayStart(date, zone), DateUtils.dayEnd(date, zone));

        int sleepPlanned = (int) Math.round(java.time.Duration.between(
                java.time.LocalTime.parse(user.getBedTimeTarget()),
                java.time.LocalTime.parse(user.getWakeTimeTarget())).toMinutes());
        if (sleepPlanned < 0)
            sleepPlanned += 1440;
        SleepRecord sleep = sleepRepository.findByUserAndDate(user, date).orElse(null);
        long sleepActual = sleep != null && sleep.getDurationMinutes() != null ? sleep.getDurationMinutes() : 0;

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row("Study", studyPlanned, studyActual));
        rows.add(row("Coding / Work", codingPlanned, codingActual));
        rows.add(row("Exercise", exercisePlanned, exerciseActual));
        rows.add(row("Sleep", sleepPlanned, sleepActual));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("date", date.toString());
        out.put("rows", rows);
        return out;
    }

    private Map<String, Object> row(String category, long planned, long actual) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("category", category);
        m.put("plannedMinutes", planned);
        m.put("actualMinutes", actual);
        m.put("differenceMinutes", actual - planned);
        return m;
    }
}