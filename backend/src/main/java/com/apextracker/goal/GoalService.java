package com.apextracker.goal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apextracker.activity.Activity;
import com.apextracker.activity.ActivityRepository;
import com.apextracker.common.ApiException;
import com.apextracker.common.DateUtils;
import com.apextracker.habit.HabitCompletionRepository;
import com.apextracker.sleep.SleepRecordRepository;
import com.apextracker.study.StudySessionRepository;
import com.apextracker.task.TaskRepository;
import com.apextracker.user.User;

@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final StudySessionRepository studyRepository;
    private final TaskRepository taskRepository;
    private final SleepRecordRepository sleepRepository;
    private final HabitCompletionRepository habitCompletionRepository;
    private final ActivityRepository activityRepository;

    public GoalService(GoalRepository goalRepository, StudySessionRepository studyRepository,
            TaskRepository taskRepository, SleepRecordRepository sleepRepository,
            HabitCompletionRepository habitCompletionRepository, ActivityRepository activityRepository) {
        this.goalRepository = goalRepository;
        this.studyRepository = studyRepository;
        this.taskRepository = taskRepository;
        this.sleepRepository = sleepRepository;
        this.habitCompletionRepository = habitCompletionRepository;
        this.activityRepository = activityRepository;
    }

    public List<GoalResponse> list(User user) {
        return goalRepository.findByUserOrderByEndDateAsc(user).stream()
                .map(g -> toResponse(user, recalc(user, g)))
                .toList();
    }

    public GoalResponse getResponse(User user, Goal goal) {
        return toResponse(user, recalc(user, goal));
    }

    @Transactional
    public Goal create(User user, GoalRequest req) {
        Goal goal = new Goal();
        goal.setUser(user);
        goal.setName(req.name().trim());
        goal.setCategory(req.category());
        goal.setTargetValue(req.targetValue());
        goal.setUnit(req.unit());
        goal.setStartDate(LocalDate.parse(req.startDate()));
        goal.setEndDate(LocalDate.parse(req.endDate()));
        if (req.manualProgress() != null) {
            goal.setProgressValue(req.manualProgress());
        }
        return goalRepository.save(goal);
    }

    @Transactional
    public Goal update(Long id, User user, GoalRequest req) {
        Goal goal = get(id, user);
        if (req.name() != null)
            goal.setName(req.name().trim());
        if (req.category() != null)
            goal.setCategory(req.category());
        if (req.targetValue() != null && req.targetValue() > 0)
            goal.setTargetValue(req.targetValue());
        if (req.unit() != null)
            goal.setUnit(req.unit());
        if (req.startDate() != null)
            goal.setStartDate(LocalDate.parse(req.startDate()));
        if (req.endDate() != null)
            goal.setEndDate(LocalDate.parse(req.endDate()));
        if (req.manualProgress() != null && goal.getCategory() == Goal.Category.CUSTOM) {
            goal.setProgressValue(req.manualProgress());
        }
        return goalRepository.save(goal);
    }

    @Transactional
    public Goal setManualProgress(Long id, User user, double value) {
        Goal goal = get(id, user);
        if (goal.getCategory() != Goal.Category.CUSTOM) {
            throw ApiException.badRequest("Progress is calculated automatically for this goal category");
        }
        goal.setProgressValue(value);
        return goalRepository.save(goal);
    }

    @Transactional
    public void delete(Long id, User user) {
        goalRepository.delete(get(id, user));
    }

    public Goal get(Long id, User user) {
        return goalRepository.findById(id)
                .filter(g -> g.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> ApiException.notFound("Goal not found"));
    }

    private Goal recalc(User user, Goal goal) {
        double progress = goal.getProgressValue();
        if (goal.getCategory() != Goal.Category.CUSTOM) {
            progress = computeProgress(user, goal);
            if (progress != goal.getProgressValue()) {
                goal.setProgressValue(progress);
            }
        }
        if (goal.getStatus() == Goal.Status.ACTIVE) {
            LocalDate now = DateUtils.today(user);
            if (!now.isBefore(goal.getEndDate())) {
                goal.setStatus(progress >= goal.getTargetValue() ? Goal.Status.ACHIEVED : Goal.Status.FAILED);
            }
            if (progress >= goal.getTargetValue()) {
                goal.setStatus(Goal.Status.ACHIEVED);
            }
        }
        return goalRepository.save(goal);
    }

    private double computeProgress(User user, Goal goal) {
        ZoneId zone = DateUtils.zone(user);
        LocalDate from = goal.getStartDate();
        LocalDate to = goal.getEndDate();
        return switch (goal.getCategory()) {
            case STUDY -> studyRepository.sumSecondsBetweenDates(user, from, to) / 60.0;
            case TASKS -> taskRepository.countByUserAndCompletedAtBetween(user,
                    DateUtils.dayStart(from, zone), DateUtils.dayEnd(to, zone));
            case SLEEP -> sleepRepository.averageDuration(user, from, to);
            case HABITS -> habitCompletionRepository.countByUserAndDateBetween(user, from, to);
            case CODING -> activityDuration(user, Activity.Category.CODING, from, to, zone);
            case READING -> activityDuration(user, Activity.Category.READING, from, to, zone);
            case EXERCISE -> activityDuration(user, Activity.Category.EXERCISING, from, to, zone);
            default -> goal.getProgressValue();
        };
    }

    private double activityDuration(User user, Activity.Category category, LocalDate from, LocalDate to, ZoneId zone) {
        return activityRepository.sumDurationByCategoryBetween(user, category,
                DateUtils.dayStart(from, zone), DateUtils.dayEnd(to, zone));
    }

    private GoalResponse toResponse(User user, Goal goal) {
        double progress = goal.getProgressValue();
        boolean minutesBased = goal.getCategory() == Goal.Category.STUDY
                || goal.getCategory() == Goal.Category.CODING
                || goal.getCategory() == Goal.Category.READING
                || goal.getCategory() == Goal.Category.EXERCISE;
        String progressLabel = minutesBased
                ? DateUtils.formatDuration((long) progress)
                : Integer.toString((int) Math.round(progress));
        String targetLabel = minutesBased
                ? DateUtils.formatDuration((long) goal.getTargetValue())
                : Integer.toString((int) Math.round(goal.getTargetValue()));
        return GoalResponse.from(goal, progress, progressLabel, targetLabel);
    }
}