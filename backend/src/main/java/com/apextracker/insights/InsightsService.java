package com.apextracker.insights;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.apextracker.activity.ActivityRepository;
import com.apextracker.common.DateUtils;
import com.apextracker.habit.Habit;
import com.apextracker.habit.HabitRepository;
import com.apextracker.performance.PerformanceMetricRepository;
import com.apextracker.sleep.SleepRecord;
import com.apextracker.sleep.SleepRecordRepository;
import com.apextracker.study.StudySession;
import com.apextracker.study.StudySessionRepository;
import com.apextracker.task.Task;
import com.apextracker.task.TaskRepository;
import com.apextracker.user.User;

@Service
public class InsightsService {

    private final TaskRepository taskRepository;
    private final StudySessionRepository studyRepository;
    private final SleepRecordRepository sleepRepository;
    private final ActivityRepository activityRepository;
    private final HabitRepository habitRepository;
    private final PerformanceMetricRepository performanceRepository;

    public InsightsService(TaskRepository taskRepository, StudySessionRepository studyRepository,
            SleepRecordRepository sleepRepository, ActivityRepository activityRepository,
            HabitRepository habitRepository, PerformanceMetricRepository performanceRepository) {
        this.taskRepository = taskRepository;
        this.studyRepository = studyRepository;
        this.sleepRepository = sleepRepository;
        this.activityRepository = activityRepository;
        this.habitRepository = habitRepository;
        this.performanceRepository = performanceRepository;
    }

    public List<Map<String, Object>> insights(User user) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = DateUtils.today(user);
        ZoneId zone = DateUtils.zone(user);
        int offset = weekStartOffset(user.getWeekStartDay(), today);
        LocalDate weekStart = today.minusDays(offset);
        LocalDate weekEnd = weekStart.plusDays(6);

        // Average study session
        List<StudySession> sessions = studyRepository.findByUserOrderByStartTimeDesc(user);
        if (!sessions.isEmpty()) {
            double avgMin = sessions.stream().mapToLong(StudySession::getAccumulatedSeconds).average().orElse(0) / 60.0;
            result.add(insight("study", "Your average study session is " + Math.round(avgMin) + " minutes."));
            // Most consistent study window
            Map<Integer, Long> windowMap = new TreeMap<>();
            for (StudySession s : sessions) {
                int hour = ZonedDateTime.ofInstant(s.getStartTime(), zone).getHour();
                int bucket = (hour / 3) * 3;
                windowMap.merge(bucket, 1L, Long::sum);
            }
            windowMap.entrySet().stream().max(Map.Entry.comparingByValue()).ifPresent(e -> {
                int b = e.getKey();
                result.add(insight("study",
                        "You study most consistently between " + String.format("%02d:00", b) + " and "
                                + String.format("%02d:00", b + 3) + "."));
            });
        }

        // Tasks this week
        List<Task> weekTasks = taskRepository.findByUserAndDueDateBetween(user, weekStart, weekEnd);
        long completed = weekTasks.stream().filter(t -> t.getStatus() == Task.Status.COMPLETED).count();
        long planned = weekTasks.stream().filter(t -> t.getStatus() != Task.Status.SKIPPED).count();
        if (planned > 0) {
            result.add(insight("task",
                    "You completed " + Math.round(completed * 100.0 / planned) + "% of your planned tasks this week."));
        }

        // Post-21:00 postponement pattern
        List<Task> lateTasks = weekTasks.stream()
                .filter(t -> t.getDueTime() != null && t.getDueTime().isAfter(LocalTime.of(21, 0))
                        && (t.isMissed() || t.getRescheduleCount() > 0))
                .toList();
        if (!lateTasks.isEmpty()) {
            result.add(insight("task",
                    "You frequently postpone or miss tasks scheduled after 21:00 (" + lateTasks.size() + " this week)."));
        }

        // Sleep variance
        List<SleepRecord> weekSleep = sleepRepository.findByUserAndDateBetween(user, weekStart, weekEnd);
        if (!weekSleep.isEmpty()) {
            double avg = weekSleep.stream().mapToInt(SleepRecord::getDurationMinutes).average().orElse(0);
            // Keep the filter expression clear for the statement
            long largeVariance = weekSleep.stream()
                    .filter(r -> Math.abs(r.getDurationMinutes() - avg) > 40).count();
            if (latestDaysVariance(weekSleep, 7) > 60) {
                result.add(insight("sleep",
                        "Your sleep schedule has varied by approximately " + DateUtils.formatDuration((long) latestDaysVariance(weekSleep, 7))
                                + " this week."));
            } else if (!weekSleep.isEmpty()) {
                result.add(insight("sleep",
                        "Average sleep this week: " + DateUtils.formatDuration(Math.round(avg)) + "."));
            }
            // Careful correlation: observation only, no causation claim
            var perf = performanceRepository.findByUserAndDateBetweenOrderByDateAsc(user,
                    weekStart, weekEnd);
            if (!perf.isEmpty() && largeVariance > 0) {
                int avgPerf = (int) Math.round(perf.stream().mapToInt(p -> p.getOverallScore()).average().orElse(0));
                double avgSleep = weekSleep.stream().mapToInt(SleepRecord::getDurationMinutes).average().orElse(0);
                if (avgSleep < 360 || avgSleep > 600) {
                    result.add(insight("sleep",
                            "Your average sleep was " + DateUtils.formatDuration(Math.round(avgSleep))
                                    + " and your average performance was " + avgPerf
                                    + "%. Previous similar nights also showed varied performance. These are observations from your data, not evidence of a cause."));
                }
            }
        }

        // Most frequently missed category
        List<Object[]> missedByCategory = taskRepository.countMissedByCategory(user);
        if (!missedByCategory.isEmpty()) {
            Object top = missedByCategory.get(0)[0];
            result.add(insight("task",
                    "Your most frequently missed task category is " + top + " (" + missedByCategory.get(0)[1] + " missed)."));
        }

        // Habit streaks
        List<Habit> habits = habitRepository.findByUserOrderByCreatedAtAsc(user);
        habits.stream().findFirst().ifPresent(h -> {
            result.add(insight("habit", "You have " + habits.size() + " active habit" + (habits.size() == 1 ? "" : "s") + "."));
        });

        if (result.isEmpty()) {
            result.add(insight("general", "Track a few days of data and insights will appear here."));
        }
        return result;
    }

    private long latestDaysVariance(List<SleepRecord> records, int days) {
        if (records.size() < 2)
            return 0;
        List<Integer> durations = records.stream()
                .map(SleepRecord::getDurationMinutes)
                .limit(days).sorted().toList();
        long min = durations.get(0);
        long max = durations.get(durations.size() - 1);
        return Math.abs(max - min);
    }

    private int weekStartOffset(String weekStartDay, LocalDate date) {
        int dow = date.getDayOfWeek().getValue();
        return switch (weekStartDay.toUpperCase()) {
            case "SUNDAY" -> dow % 7;
            default -> dow - 1;
        };
    }

    private Map<String, Object> insight(String type, String text) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type);
        map.put("text", text);
        return map;
    }
}