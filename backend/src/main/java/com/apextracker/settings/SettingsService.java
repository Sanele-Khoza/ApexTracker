package com.apextracker.settings;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apextracker.activity.Activity;
import com.apextracker.activity.ActivityRepository;
import com.apextracker.common.DateUtils;
import com.apextracker.goal.Goal;
import com.apextracker.goal.GoalRepository;
import com.apextracker.habit.Habit;
import com.apextracker.habit.HabitRepository;
import com.apextracker.performance.PerformanceMetricRepository;
import com.apextracker.reminder.ReminderRepository;
import com.apextracker.review.DailyReview;
import com.apextracker.review.DailyReviewRepository;
import com.apextracker.sleep.SleepRecord;
import com.apextracker.sleep.SleepRecordRepository;
import com.apextracker.study.StudySession;
import com.apextracker.study.StudySessionRepository;
import com.apextracker.task.Task;
import com.apextracker.task.TaskRepository;
import com.apextracker.user.User;
import com.apextracker.user.UserRepository;

@Service
public class SettingsService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ActivityRepository activityRepository;
    private final StudySessionRepository studyRepository;
    private final SleepRecordRepository sleepRepository;
    private final HabitRepository habitRepository;
    private final GoalRepository goalRepository;
    private final ReminderRepository reminderRepository;
    private final DailyReviewRepository reviewRepository;
    private final PerformanceMetricRepository performanceRepository;

    public SettingsService(UserRepository userRepository, TaskRepository taskRepository,
            ActivityRepository activityRepository, StudySessionRepository studyRepository,
            SleepRecordRepository sleepRepository, HabitRepository habitRepository, GoalRepository goalRepository,
            ReminderRepository reminderRepository, DailyReviewRepository reviewRepository,
            PerformanceMetricRepository performanceRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.activityRepository = activityRepository;
        this.studyRepository = studyRepository;
        this.sleepRepository = sleepRepository;
        this.habitRepository = habitRepository;
        this.goalRepository = goalRepository;
        this.reminderRepository = reminderRepository;
        this.reviewRepository = reviewRepository;
        this.performanceRepository = performanceRepository;
    }

    @Transactional
    public User update(User user, SettingsUpdateRequest req) {
        if (req.name() != null && !req.name().isBlank())
            user.setName(req.name().trim());
        if (req.timezone() != null && !req.timezone().isBlank()) {
            try {
                java.time.ZoneId.of(req.timezone());
                user.setTimezone(req.timezone());
            } catch (Exception e) {
                throw com.apextracker.common.ApiException.badRequest("Timezone is invalid. Use a value like " + java.time.ZoneId.systemDefault());
            }
        }
        if (req.weekStartDay() != null && !req.weekStartDay().isBlank())
            user.setWeekStartDay(req.weekStartDay().toUpperCase());
        if (req.bedTimeTarget() != null && !req.bedTimeTarget().isBlank())
            user.setBedTimeTarget(DateUtils.parseTime(req.bedTimeTarget()).toString());
        if (req.wakeTimeTarget() != null && !req.wakeTimeTarget().isBlank())
            user.setWakeTimeTarget(DateUtils.parseTime(req.wakeTimeTarget()).toString());
        if (req.weeklyStudyGoalMinutes() != null)
            user.setWeeklyStudyGoalMinutes(req.weeklyStudyGoalMinutes());
        if (req.quietHoursStart() != null && !req.quietHoursStart().isBlank())
            user.setQuietHoursStart(DateUtils.parseTime(req.quietHoursStart()).toString());
        if (req.quietHoursEnd() != null && !req.quietHoursEnd().isBlank())
            user.setQuietHoursEnd(DateUtils.parseTime(req.quietHoursEnd()).toString());
        if (req.theme() != null && !req.theme().isBlank())
            user.setTheme(req.theme());
        if (req.emailNotifications() != null)
            user.setEmailNotifications(req.emailNotifications());
        if (req.reminderEnabled() != null)
            user.setReminderEnabled(req.reminderEnabled());
        if (req.reminderDefaultMinutes() != null)
            user.setReminderDefaultMinutes(req.reminderDefaultMinutes());
        return userRepository.save(user);
    }

    public Map<String, Object> export(User user) {
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("exportedAt", Instant.now().toString());
        export.put("user", Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail(),
                "timezone", user.getTimezone(), "weekStartDay", user.getWeekStartDay(),
                "bedTimeTarget", user.getBedTimeTarget(), "wakeTimeTarget", user.getWakeTimeTarget(),
                "weeklyStudyGoalMinutes", user.getWeeklyStudyGoalMinutes(),
                "quietHours", List.of(user.getQuietHoursStart(), user.getQuietHoursEnd())));

        List<Map<String, Object>> tasks = taskRepository.findByUserOrderByDueDateAscDueTimeAsc(user).stream()
                .map(t -> item(t.getId(), "name", t.getName(), "category", t.getCategory(), "priority", t.getPriority(),
                        "status", t.getStatus(), "dueDate", t.getDueDate(), "dueTime", t.getDueTime(),
                        "estimatedMinutes", t.getEstimatedMinutes(), "recurrence", t.getRecurrence(),
                        "missed", t.isMissed(), "completedAt", t.getCompletedAt()))
                .toList();
        export.put("tasks", tasks);

        List<Map<String, Object>> activities = activityRepository.findByUserOrderByStartTimeDesc(user).stream()
                .map(a -> item(a.getId(), "name", a.getName(), "category", a.getCategory(), "startTime",
                        a.getStartTime(), "endTime", a.getEndTime(), "durationMinutes", a.getDurationMinutes(),
                        "productivityRating", a.getProductivityRating(), "notes", a.getNotes()))
                .toList();
        export.put("activities", activities);

        List<Map<String, Object>> study = studyRepository.findByUserOrderByStartTimeDesc(user).stream()
                .map(s -> item(s.getId(), "subject", s.getSubject(), "topic", s.getTopic(), "startTime",
                        s.getStartTime(), "endTime", s.getEndTime(), "durationSeconds", s.getAccumulatedSeconds(),
                        "method", s.getMethod(), "focusRating", s.getFocusRating(), "notes", s.getNotes()))
                .toList();
        export.put("studySessions", study);

        List<Map<String, Object>> sleep = sleepRepository.findByUserOrderByDateDesc(user).stream()
                .map(s -> item(s.getId(), "date", s.getDate(), "bedTime", s.getBedTime(), "sleepTime", s.getSleepTime(),
                        "wakeTime", s.getWakeTime(), "durationMinutes", s.getDurationMinutes(), "quality", s.getQuality(),
                        "notes", s.getNotes()))
                .toList();
        export.put("sleep", sleep);

        List<Map<String, Object>> habits = habitRepository.findByUserOrderByCreatedAtAsc(user).stream()
                .map(h -> item(h.getId(), "name", h.getName(), "frequency", h.getFrequency(), "target", h.getTarget(),
                        "reminderMinutes", h.getReminderMinutes()))
                .toList();
        export.put("habits", habits);

        List<Map<String, Object>> goals = goalRepository.findByUserOrderByEndDateAsc(user).stream()
                .map(g -> item(g.getId(), "name", g.getName(), "category", g.getCategory(), "targetValue",
                        g.getTargetValue(), "progressValue", g.getProgressValue(), "unit", g.getUnit(),
                        "startDate", g.getStartDate(), "endDate", g.getEndDate(), "status", g.getStatus()))
                .toList();
        export.put("goals", goals);

        List<Map<String, Object>> metrics = performanceRepository
                .findByUserAndDateBetweenOrderByDateAsc(user, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1))
                .stream()
                .map(m -> item(m.getId(), "date", m.getDate(), "productivity", m.getProductivityScore(),
                        "study", m.getStudyScore(), "routine", m.getRoutineScore(), "sleep", m.getSleepScore(),
                        "overall", m.getOverallScore()))
                .toList();
        export.put("performance", metrics);

        List<Map<String, Object>> reviews = reviewRepository.findByUserOrderByDateDesc(user).stream()
                .map(r -> item(r.getId(), "date", r.getDate(), "reflection", r.getReflection(), "summary", r.getSummary()))
                .toList();
        export.put("dailyReviews", reviews);

        return export;
    }

    @Transactional
    public void deleteAccount(User user) {
        taskRepository.deleteByUser(user);
        activityRepository.deleteByUser(user);
        studyRepository.deleteByUser(user);
        sleepRepository.deleteByUser(user);
        habitRepository.deleteByUser(user);
        goalRepository.deleteByUser(user);
        reminderRepository.deleteByUser(user);
        reviewRepository.deleteByUser(user);
        performanceRepository.deleteByUser(user);
        userRepository.delete(user);
    }

    private Map<String, Object> item(Long id, Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        for (int i = 0; i < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }
}