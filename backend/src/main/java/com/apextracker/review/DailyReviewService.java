package com.apextracker.review;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apextracker.activity.ActivityRepository;
import com.apextracker.common.DateUtils;
import com.apextracker.habit.Habit;
import com.apextracker.habit.HabitRepository;
import com.apextracker.performance.PerformanceMetric;
import com.apextracker.performance.PerformanceService;
import com.apextracker.sleep.SleepRecord;
import com.apextracker.sleep.SleepRecordRepository;
import com.apextracker.study.StudySessionRepository;
import com.apextracker.task.Task;
import com.apextracker.task.TaskRepository;
import com.apextracker.user.User;

@Service
public class DailyReviewService {

    private final DailyReviewRepository reviewRepository;
    private final TaskRepository taskRepository;
    private final StudySessionRepository studyRepository;
    private final SleepRecordRepository sleepRepository;
    private final ActivityRepository activityRepository;
    private final HabitRepository habitRepository;
    private final PerformanceService performanceService;

    public DailyReviewService(DailyReviewRepository reviewRepository, TaskRepository taskRepository,
            StudySessionRepository studyRepository, SleepRecordRepository sleepRepository,
            ActivityRepository activityRepository, HabitRepository habitRepository,
            PerformanceService performanceService) {
        this.reviewRepository = reviewRepository;
        this.taskRepository = taskRepository;
        this.studyRepository = studyRepository;
        this.sleepRepository = sleepRepository;
        this.activityRepository = activityRepository;
        this.habitRepository = habitRepository;
        this.performanceService = performanceService;
    }

    @Transactional
    public DailyReview getOrGenerate(User user, LocalDate date) {
        return reviewRepository.findByUserAndDate(user, date)
                .orElseGet(() -> {
                    DailyReview review = new DailyReview();
                    review.setUser(user);
                    review.setDate(date);
                    review.setSummary(generateSummary(user, date));
                    return reviewRepository.save(review);
                });
    }

    @Transactional
    public DailyReview updateReflection(User user, LocalDate date, String reflection) {
        DailyReview review = getOrGenerate(user, date);
        review.setReflection(reflection);
        return reviewRepository.save(review);
    }

    public List<DailyReview> list(User user) {
        return reviewRepository.findByUserOrderByDateDesc(user);
    }

    private String generateSummary(User user, LocalDate date) {
        long completed = taskRepository.findByUserAndDueDate(user, date).stream()
                .filter(t -> t.getStatus() == Task.Status.COMPLETED).count();
        long missed = taskRepository.findByUserAndDueDate(user, date).stream()
                .filter(t -> t.isMissed()).count();
        long studySeconds = studyRepository.sumSecondsBetweenDates(user, date, date);
        long activityMinutes = activityRepository.findByUserAndStartTimeBetween(user,
                DateUtils.dayStart(date, DateUtils.zone(user)), DateUtils.dayEnd(date, DateUtils.zone(user)))
                .stream().mapToLong(a -> a.getDurationMinutes() == null ? 0 : a.getDurationMinutes()).sum();
        List<SleepRecord> sleeps = sleepRepository.findByUserAndDateBetween(user, date, date);
        boolean hasSleep = !sleeps.isEmpty();
        long habitsCount = habitRepository.findByUserOrderByCreatedAtAsc(user).size();
        PerformanceMetric metric = performanceService.computeForDate(user, date);

        StringBuilder sb = new StringBuilder();
        sb.append("Tasks: ").append(completed).append(" completed, ").append(missed).append(" missed. ");
        sb.append("Study: ").append(DateUtils.formatDuration(studySeconds / 60)).append(". ");
        sb.append("Activities: ").append(DateUtils.formatDuration(activityMinutes)).append(". ");
        sb.append("Sleep: ").append(hasSleep ? DateUtils.formatDuration(sleeps.get(0).getDurationMinutes())
                : "not recorded").append(". ");
        sb.append("Habits: ").append(habitsCount).append(" tracked. ");
        sb.append("Productivity: ").append(metric.getProductivityScore()).append("%. ");
        sb.append("Performance: ").append(metric.getOverallScore()).append(" (app-generated indicator).");
        return sb.toString();
    }
}