package com.apextracker.analytics;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.apextracker.activity.Activity;
import com.apextracker.activity.ActivityRepository;
import com.apextracker.common.DateUtils;
import com.apextracker.habit.Habit;
import com.apextracker.habit.HabitCompletionRepository;
import com.apextracker.habit.HabitRepository;
import com.apextracker.performance.PerformanceMetric;
import com.apextracker.performance.PerformanceMetricRepository;
import com.apextracker.sleep.SleepRecord;
import com.apextracker.sleep.SleepRecordRepository;
import com.apextracker.study.StudySession;
import com.apextracker.study.StudySessionRepository;
import com.apextracker.task.Task;
import com.apextracker.task.TaskRepository;
import com.apextracker.user.User;

@Service
public class AnalyticsService {

    private final TaskRepository taskRepository;
    private final StudySessionRepository studyRepository;
    private final SleepRecordRepository sleepRepository;
    private final ActivityRepository activityRepository;
    private final HabitRepository habitRepository;
    private final HabitCompletionRepository habitCompletionRepository;
    private final PerformanceMetricRepository performanceRepository;

    public AnalyticsService(TaskRepository taskRepository, StudySessionRepository studyRepository,
            SleepRecordRepository sleepRepository, ActivityRepository activityRepository,
            HabitRepository habitRepository, HabitCompletionRepository habitCompletionRepository,
            PerformanceMetricRepository performanceRepository) {
        this.taskRepository = taskRepository;
        this.studyRepository = studyRepository;
        this.sleepRepository = sleepRepository;
        this.activityRepository = activityRepository;
        this.habitRepository = habitRepository;
        this.habitCompletionRepository = habitCompletionRepository;
        this.performanceRepository = performanceRepository;
    }

    public Map<String, Object> summary(User user, String from, String to) {
        ZoneId zone = DateUtils.zone(user);
        LocalDate start = from != null ? LocalDate.parse(from) : DateUtils.today(user).minusDays(13);
        LocalDate end = to != null ? LocalDate.parse(to) : DateUtils.today(user);
        start = start.isAfter(end) ? end : start;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("from", start.toString());
        out.put("to", end.toString());
        out.put("productivity", productivity(user, start, end));
        out.put("study", study(user, start, end, zone));
        out.put("sleep", sleep(user, start, end));
        out.put("activities", activities(user, start, end, zone));
        out.put("habits", habits(user, start, end));
        out.put("performance", performance(user, start, end));
        return out;
    }

    private Map<String, Object> productivity(User user, LocalDate start, LocalDate end) {
        List<Map<String, Object>> series = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            List<Task> tasks = taskRepository.findByUserAndDueDate(user, d);
            long planned = tasks.stream().filter(t -> t.getStatus() != Task.Status.SKIPPED).count();
            long completed = tasks.stream().filter(t -> t.getStatus() == Task.Status.COMPLETED).count();
            long missed = tasks.stream().filter(Task::isMissed).count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", d.toString());
            row.put("planned", planned);
            row.put("completed", completed);
            row.put("missed", missed);
            row.put("completionRate", planned == 0 ? null : Math.round(completed * 100.0 / planned));
            series.add(row);
        }
        long totalPlanned = series.stream().mapToLong(r -> (Long) r.get("planned")).sum();
        long totalCompleted = series.stream().mapToLong(r -> (Long) r.get("completed")).sum();
        long totalMissed = series.stream().mapToLong(r -> (Long) r.get("missed")).sum();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("series", series);
        out.put("totalPlanned", totalPlanned);
        out.put("totalCompleted", totalCompleted);
        out.put("totalMissed", totalMissed);
        out.put("completionRate", totalPlanned == 0 ? null : Math.round(totalCompleted * 100.0 / totalPlanned));
        return out;
    }

    private Map<String, Object> study(User user, LocalDate start, LocalDate end, ZoneId zone) {
        List<Map<String, Object>> daily = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", d.toString());
            row.put("minutes", Math.round(studyRepository.sumSecondsBetweenDates(user, d, d) / 60.0));
            daily.add(row);
        }
        List<Object[]> bySubject = studyRepository.sumSecondsBySubject(user, start, end);
        Map<String, Object> subjects = new LinkedHashMap<>();
        for (Object[] e : bySubject) {
            subjects.put(String.valueOf(e[0]), Math.round((Long) e[1] / 60.0));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("daily", daily);
        out.put("bySubject", subjects);
        out.put("totalMinutes", Math.round(studyRepository.sumSecondsBetweenDates(user, start, end) / 60.0));
        List<StudySession> sessions = studyRepository.findByUserAndStartTimeBetween(user,
                DateUtils.dayStart(start, zone), DateUtils.dayEnd(end, zone));
        out.put("sessions", sessions.size());
        out.put("avgSessionMinutes", sessions.isEmpty() ? 0 : Math.round(sessions.stream()
                .mapToLong(StudySession::getAccumulatedSeconds).average().orElse(0) / 60.0));
        out.put("avgFocus", sessions.isEmpty() ? null : sessions.stream()
                .filter(s -> s.getFocusRating() != null).mapToInt(s -> s.getFocusRating()).average().orElse(0));
        return out;
    }

    private Map<String, Object> sleep(User user, LocalDate start, LocalDate end) {
        List<Map<String, Object>> series = new ArrayList<>();
        List<SleepRecord> records = sleepRepository.findByUserAndDateBetween(user, start, end);
        for (SleepRecord r : records) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", r.getDate().toString());
            row.put("durationMinutes", r.getDurationMinutes());
            row.put("quality", r.getQuality());
            series.add(row);
        }
        double avg = sleepRepository.averageDuration(user, start, end);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("series", series);
        out.put("avgDurationMinutes", Math.round(avg));
        out.put("daysTracked", records.size());
        out.put("consistencyPercent", computeConsistency(user, start, end));
        return out;
    }

    private int computeConsistency(User user, LocalDate start, LocalDate end) {
        List<SleepRecord> records = sleepRepository.findByUserAndDateBetween(user, start, end);
        if (records.isEmpty())
            return 0;
        double avg = records.stream().filter(r -> r.getDurationMinutes() != null).mapToInt(SleepRecord::getDurationMinutes)
                .average().orElse(0);
        if (avg == 0)
            return 0;
        int days = (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        // consistency = % of days tracked with duration close to avg (+- 60 min)
        double stable = records.stream().filter(r -> r.getDurationMinutes() != null
                && Math.abs(r.getDurationMinutes() - avg) <= 60).count();
        return (int) Math.round(stable / days * 100);
    }

    private Map<String, Object> activities(User user, LocalDate start, LocalDate end, ZoneId zone) {
        List<Activity> list = activityRepository.findByUserAndStartTimeBetween(user,
                DateUtils.dayStart(start, zone), DateUtils.dayEnd(end, zone));
        Map<String, Object> byCategory = new LinkedHashMap<>();
        long totalMinutes = 0;
        long productiveMinutes = 0;
        for (Activity a : list) {
            int minutes = a.getDurationMinutes() == null ? 0 : a.getDurationMinutes();
            totalMinutes += minutes;
            byCategory.merge(a.getCategory().name(), minutes, (x, y) -> (int) x + (int) y);
            boolean productive = a.getCategory() == Activity.Category.STUDYING
                    || a.getCategory() == Activity.Category.CODING
                    || a.getCategory() == Activity.Category.READING
                    || a.getCategory() == Activity.Category.WORKING
                    || a.getCategory() == Activity.Category.EXERCISING
                    || (a.getProductivityRating() != null && a.getProductivityRating() >= 4);
            if (productive)
                productiveMinutes += minutes;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("byCategory", byCategory);
        out.put("totalMinutes", totalMinutes);
        out.put("productiveMinutes", productiveMinutes);
        out.put("nonProductiveMinutes", totalMinutes - productiveMinutes);
        out.put("timeline", timeline(list, zone));
        return out;
    }

    private List<Map<String, Object>> timeline(List<Activity> list, ZoneId zone) {
        return list.stream().filter(a -> a.getEndTime() != null).sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .map(a -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", a.getName());
                    row.put("category", a.getCategory().name());
                    row.put("start", a.getStartTime().atZone(zone).toString());
                    row.put("end", a.getEndTime().atZone(zone).toString());
                    row.put("durationMinutes", a.getDurationMinutes());
                    return row;
                }).toList();
    }

    private Map<String, Object> habits(User user, LocalDate start, LocalDate end) {
        List<Habit> habits = habitRepository.findByUserOrderByCreatedAtAsc(user);
        int days = (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        long expected = habits.stream().filter(h -> h.getFrequency() == Habit.Frequency.DAILY)
                .mapToLong(Habit::getTarget).sum() * days;
        long actual = habitCompletionRepository.countByUserAndDateBetween(user, start, end);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("expected", expected);
        out.put("actual", actual);
        out.put("completionRate", expected == 0 ? null : Math.round(actual * 100.0 / expected));
        return out;
    }

    private Map<String, Object> performance(User user, LocalDate start, LocalDate end) {
        List<PerformanceMetric> metrics = performanceRepository
                .findByUserAndDateBetweenOrderByDateAsc(user, start, end);
        List<Map<String, Object>> series = new ArrayList<>();
        for (PerformanceMetric m : metrics) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", m.getDate().toString());
            row.put("productivity", m.getProductivityScore());
            row.put("study", m.getStudyScore());
            row.put("routine", m.getRoutineScore());
            row.put("sleepScore", m.getSleepScore());
            row.put("overall", m.getOverallScore());
            series.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("series", series);
        out.put("avgOverall", metrics.isEmpty() ? null
                : Math.round(metrics.stream().mapToInt(PerformanceMetric::getOverallScore).average().orElse(0)));
        return out;
    }
}