package com.apextracker.analytics;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apextracker.activity.ActivityRepository;
import com.apextracker.common.DateUtils;
import com.apextracker.config.CurrentUserService;
import com.apextracker.goal.GoalRepository;
import com.apextracker.habit.HabitCompletionRepository;
import com.apextracker.habit.HabitRepository;
import com.apextracker.performance.PerformanceMetricRepository;
import com.apextracker.performance.PerformanceService;
import com.apextracker.sleep.SleepRecordRepository;
import com.apextracker.study.StudySessionRepository;
import com.apextracker.task.Task;
import com.apextracker.task.TaskRepository;
import com.apextracker.user.User;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CurrentUserService currentUser;
    private final TaskRepository taskRepository;
    private final StudySessionRepository studyRepository;
    private final SleepRecordRepository sleepRepository;
    private final HabitRepository habitRepository;
    private final HabitCompletionRepository habitCompletionRepository;
    private final GoalRepository goalRepository;
    private final PerformanceMetricRepository performanceRepository;
    private final PerformanceService performanceService;
    private final ActivityRepository activityRepository;

    public AnalyticsController(AnalyticsService analyticsService, CurrentUserService currentUser,
            TaskRepository taskRepository, StudySessionRepository studyRepository, SleepRecordRepository sleepRepository,
            HabitRepository habitRepository, HabitCompletionRepository habitCompletionRepository,
            GoalRepository goalRepository, PerformanceMetricRepository performanceRepository,
            PerformanceService performanceService, ActivityRepository activityRepository) {
        this.analyticsService = analyticsService;
        this.currentUser = currentUser;
        this.taskRepository = taskRepository;
        this.studyRepository = studyRepository;
        this.sleepRepository = sleepRepository;
        this.habitRepository = habitRepository;
        this.habitCompletionRepository = habitCompletionRepository;
        this.goalRepository = goalRepository;
        this.performanceRepository = performanceRepository;
        this.performanceService = performanceService;
        this.activityRepository = activityRepository;
    }

    @GetMapping
    public Map<String, Object> analytics(@RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return analyticsService.summary(currentUser.get(), from, to);
    }

    @GetMapping("/weekly")
    public Map<String, Object> weekly() {
        User user = currentUser.get();
        LocalDate today = DateUtils.today(user);
        int offset = performanceService.weekStartOffset(user, today);
        LocalDate weekStart = today.minusDays(offset);
        LocalDate weekEnd = weekStart.plusDays(6);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("weekStart", weekStart.toString());
        out.put("weekEnd", weekEnd.toString());

        long completed = taskRepository.findByUserAndDueDateBetween(user, weekStart, weekEnd).stream()
                .filter(t -> t.getStatus() == Task.Status.COMPLETED).count();
        long missed = taskRepository.findByUserAndDueDateBetween(user, weekStart, weekEnd).stream()
                .filter(Task::isMissed).count();
        long planned = taskRepository.findByUserAndDueDateBetween(user, weekStart, weekEnd).stream()
                .filter(t -> t.getStatus() != Task.Status.SKIPPED).count();
        out.put("taskCompleted", completed);
        out.put("taskMissed", missed);
        out.put("taskCompletion", planned == 0 ? 100 : Math.round(completed * 100.0 / planned));

        long study = Math.round(studyRepository.sumSecondsBetweenDates(user, weekStart, weekEnd) / 60.0);
        int studyGoal = user.getWeeklyStudyGoalMinutes();
        out.put("studyMinutes", study);
        out.put("studyGoalMinutes", studyGoal);
        out.put("studyGoalPct", studyGoal == 0 ? 0 : Math.round(study * 100.0 / studyGoal));

        double avgSleep = sleepRepository.averageDuration(user, weekStart, weekEnd);
        out.put("sleepAvgMinutes", Math.round(avgSleep));
        out.put("sleepTargetMinutes", targetSleepMinutes(user));

        long habitExpected = habitRepository.findByUserOrderByCreatedAtAsc(user).stream()
                .filter(h -> h.getFrequency() == com.apextracker.habit.Habit.Frequency.DAILY)
                .mapToLong(com.apextracker.habit.Habit::getTarget).sum() * 7;
        long habitActual = habitCompletionRepository.countByUserAndDateBetween(user, weekStart, weekEnd);
        out.put("habitCompletion", habitExpected == 0 ? 100 : Math.round(habitActual * 100.0 / habitExpected));

        var metrics = performanceRepository.findByUserAndDateBetweenOrderByDateAsc(user, weekStart, weekEnd);
        out.put("avgPerformance", metrics.isEmpty() ? null
                : Math.round(metrics.stream().mapToInt(m -> m.getOverallScore()).average().orElse(0)));

        out.put("mainImprovementArea", mainImprovementArea(user, weekStart));

        var goals = goalRepository.findByUserOrderByEndDateAsc(user);
        out.put("activeGoals", goals.stream().filter(g -> g.getStatus() == com.apextracker.goal.Goal.Status.ACTIVE).count());
        out.put("achievedGoals", goals.stream().filter(g -> g.getStatus() == com.apextracker.goal.Goal.Status.ACHIEVED).count());
        return out;
    }

    private String mainImprovementArea(User user, LocalDate weekStart) {
        long morningMissed = taskRepository.findByUserAndDueDateBetween(user, weekStart, weekStart.plusDays(6)).stream()
                .filter(t -> t.isMissed() && t.getDueTime() != null && t.getDueTime().isBefore(java.time.LocalTime.NOON))
                .count();
        if (morningMissed > 0) {
            return "Completing tasks scheduled in the morning.";
        }
        long eveningMissed = taskRepository.findByUserAndDueDateBetween(user, weekStart, weekStart.plusDays(6)).stream()
                .filter(t -> t.isMissed() && t.getDueTime() != null && t.getDueTime().isAfter(java.time.LocalTime.of(21, 0)))
                .count();
        return eveningMissed > 0 ? "Completing tasks scheduled after 21:00." : "Maintaining consistent study time.";
    }

    private long targetSleepMinutes(User user) {
        java.time.LocalTime bed = java.time.LocalTime.parse(user.getBedTimeTarget());
        java.time.LocalTime wake = java.time.LocalTime.parse(user.getWakeTimeTarget());
        long minutes = java.time.temporal.ChronoUnit.MINUTES.between(bed, wake);
        return minutes < 0 ? minutes + 1440 : minutes;
    }
}