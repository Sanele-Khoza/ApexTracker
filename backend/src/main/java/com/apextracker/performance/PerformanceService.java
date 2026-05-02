package com.apextracker.performance;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apextracker.activity.Activity;
import com.apextracker.activity.ActivityRepository;
import com.apextracker.common.DateUtils;
import com.apextracker.habit.Habit;
import com.apextracker.habit.HabitCompletionRepository;
import com.apextracker.habit.HabitRepository;
import com.apextracker.sleep.SleepRecord;
import com.apextracker.sleep.SleepRecordRepository;
import com.apextracker.study.StudySessionRepository;
import com.apextracker.task.Task;
import com.apextracker.task.TaskRepository;
import com.apextracker.user.User;

@Service
public class PerformanceService {

    private final PerformanceMetricRepository performanceRepository;
    private final TaskRepository taskRepository;
    private final StudySessionRepository studyRepository;
    private final SleepRecordRepository sleepRepository;
    private final HabitRepository habitRepository;
    private final HabitCompletionRepository habitCompletionRepository;
    private final ActivityRepository activityRepository;
    private final tools.jackson.databind.ObjectMapper objectMapper;

    public PerformanceService(PerformanceMetricRepository performanceRepository, TaskRepository taskRepository,
            StudySessionRepository studyRepository, SleepRecordRepository sleepRepository,
            HabitRepository habitRepository, HabitCompletionRepository habitCompletionRepository,
            ActivityRepository activityRepository, tools.jackson.databind.ObjectMapper objectMapper) {
        this.performanceRepository = performanceRepository;
        this.taskRepository = taskRepository;
        this.studyRepository = studyRepository;
        this.sleepRepository = sleepRepository;
        this.habitRepository = habitRepository;
        this.habitCompletionRepository = habitCompletionRepository;
        this.activityRepository = activityRepository;
        this.objectMapper = objectMapper;
    }

    public PerformanceMetric computeForDate(User user, LocalDate date) {
        return performanceRepository.findByUserAndDate(user, date)
                .orElseGet(() -> computeAndStore(user, date));
    }

    public PerformanceMetric computeToday(User user) {
        return computeForDate(user, DateUtils.today(user));
    }

    public PerformanceMetric latest(User user) {
        return performanceRepository.findTopByUserAndDateBeforeOrderByDateDesc(user, DateUtils.today(user))
                .orElseGet(() -> computeToday(user));
    }

    @Transactional
    public PerformanceMetric computeAndStore(User user, LocalDate date) {
        ZoneId zone = DateUtils.zone(user);

        List<Task> dueTasks = taskRepository.findByUserAndDueDate(user, date);
        long planned = dueTasks.stream().filter(t -> t.getStatus() != Task.Status.SKIPPED).count();
        long completed = dueTasks.stream().filter(t -> t.getStatus() == Task.Status.COMPLETED).count();
        int productivity = planned == 0 ? 100 : (int) Math.round(completed * 100.0 / planned);

        int weekOffset = weekStartOffset(user, date);
        LocalDate weekStart = date.minusDays(weekOffset);
        LocalDate weekEnd = weekStart.plusDays(6);
        long studySeconds = studyRepository.sumSecondsBetweenDates(user, weekStart, weekEnd);
        int weeklyGoal = Math.max(1, user.getWeeklyStudyGoalMinutes() * 60);
        int studyScore = (int) Math.min(100, Math.round(studySeconds * 100.0 / weeklyGoal));

        List<Habit> habits = habitRepository.findByUserOrderByCreatedAtAsc(user);
        long expectedToday = habits.stream()
                .filter(h -> h.getFrequency() == Habit.Frequency.DAILY)
                .mapToLong(Habit::getTarget).sum();
        long completedHabits = habitCompletionRepository.countByUserAndDateBetween(user, date, date);
        int routineScore = expectedToday == 0 ? 100
                : (int) Math.round(Math.min(100, completedHabits * 100.0 / expectedToday));

        int sleepScore = computeSleepScore(user, date);

        List<Activity> activities = activityRepository.findByUserAndStartTimeBetween(user,
                DateUtils.dayStart(date, zone), DateUtils.dayEnd(date, zone));
        double ratingAvg = activities.stream()
                .filter(a -> a.getProductivityRating() != null)
                .mapToInt(Activity::getProductivityRating).average().orElse(0);
        int activityScore = activities.isEmpty() ? 50 : (int) Math.round(ratingAvg * 20.0);

        int overall = (int) Math.round(productivity * 0.30 + studyScore * 0.25 + routineScore * 0.20
                + sleepScore * 0.15 + activityScore * 0.10);

        PerformanceMetric metric = new PerformanceMetric();
        metric.setUser(user);
        metric.setDate(date);
        metric.setProductivityScore(productivity);
        metric.setStudyScore(studyScore);
        metric.setRoutineScore(routineScore);
        metric.setSleepScore(sleepScore);
        metric.setActivityScore(activityScore);
        metric.setOverallScore(overall);
        try {
            metric.setDetails(objectMapper.writeValueAsString(Map.of(
                    "planned", planned,
                    "completed", completed,
                    "studySecondsWeek", studySeconds,
                    "completedHabits", completedHabits,
                    "expectedHabits", expectedToday)));
        } catch (Exception ignored) {
        }
        return performanceRepository.save(metric);
    }

    private int sleepScoreFallback = 50;

    private int computeSleepScore(User user, LocalDate date) {
        var optional = sleepRepository.findByUserAndDate(user, date);
        if (optional.isEmpty()) {
            return sleepScoreFallback;
        }
        SleepRecord record = optional.get();
        if (record.getDurationMinutes() == null) {
            return sleepScoreFallback;
        }
        int penalty = 0;
        try {
            LocalTime targetBed = DateUtils.parseTime(user.getBedTimeTarget());
            LocalTime targetWake = DateUtils.parseTime(user.getWakeTimeTarget());
            if (record.getBedTime() != null) {
                int delta = timeDeltaMinutes(record.getBedTime(), targetBed);
                if (Math.abs(delta) > 60)
                    penalty += 30;
                else if (Math.abs(delta) > 30)
                    penalty += 15;
            }
            int delta = timeDeltaMinutes(record.getWakeTime(), targetWake);
            if (Math.abs(delta) > 45)
                penalty += 20;
            else if (Math.abs(delta) > 20)
                penalty += 10;
        } catch (Exception ignored) {
        }
        int duration = record.getDurationMinutes();
        if (duration < 360)
            penalty += 30;
        else if (duration > 600)
            penalty += 20;
        else if (duration < 420 || duration > 540)
            penalty += 10;
        return Math.max(0, 100 - penalty);
    }

    private int timeDeltaMinutes(LocalTime actual, LocalTime target) {
        return actual.getHour() * 60 + actual.getMinute() - (target.getHour() * 60 + target.getMinute());
    }

    public int weekStartOffset(User user, LocalDate date) {
        int dow = date.getDayOfWeek().getValue();
        return switch (user.getWeekStartDay().toUpperCase()) {
            case "SUNDAY" -> dow % 7;
            default -> dow - 1;
        };
    }
}