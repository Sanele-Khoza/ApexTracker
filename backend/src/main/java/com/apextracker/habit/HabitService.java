package com.apextracker.habit;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apextracker.common.ApiException;
import com.apextracker.common.DateUtils;
import com.apextracker.user.User;

@Service
public class HabitService {

    private final HabitRepository habitRepository;
    private final HabitCompletionRepository completionRepository;

    public HabitService(HabitRepository habitRepository, HabitCompletionRepository completionRepository) {
        this.habitRepository = habitRepository;
        this.completionRepository = completionRepository;
    }

    public List<HabitResponse> list(User user) {
        LocalDate today = DateUtils.today(user);
        List<Habit> habits = habitRepository.findByUserOrderByCreatedAtAsc(user);
        int offset = startOffset(user);
        return habits.stream().map(h -> toResponse(h, today, offset)).toList();
    }

    public HabitResponse getResponse(User user, Habit habit) {
        return toResponse(habit, DateUtils.today(user), startOffset(user));
    }

    @Transactional
    public Habit create(User user, HabitRequest req) {
        Habit habit = new Habit();
        habit.setUser(user);
        habit.setName(req.name().trim());
        habit.setFrequency(req.frequency());
        habit.setTarget(req.target());
        habit.setReminderMinutes(req.reminderMinutes());
        return habitRepository.save(habit);
    }

    @Transactional
    public Habit update(Long id, User user, HabitRequest req) {
        Habit habit = get(id, user);
        if (req.name() != null)
            habit.setName(req.name().trim());
        if (req.frequency() != null)
            habit.setFrequency(req.frequency());
        if (req.target() != null && req.target() >= 1)
            habit.setTarget(req.target());
        if (req.reminderMinutes() != null)
            habit.setReminderMinutes(req.reminderMinutes());
        return habitRepository.save(habit);
    }

    @Transactional
    public void delete(Long id, User user) {
        Habit habit = get(id, user);
        completionRepository.deleteByHabit(habit);
        habitRepository.delete(habit);
    }

    @Transactional
    public HabitResponse toggle(Long id, User user, String dateStr) {
        Habit habit = get(id, user);
        LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : DateUtils.today(user);
        boolean nowCompleted = completionRepository.existsByHabitAndDate(habit, date);
        if (nowCompleted) {
            completionRepository.findByHabitAndDate(habit, date).ifPresent(completionRepository::delete);
        } else {
            HabitCompletion completion = new HabitCompletion();
            completion.setHabit(habit);
            completion.setDate(date);
            completionRepository.save(completion);
        }
        return getResponse(user, habit);
    }

    public Habit get(Long id, User user) {
        return habitRepository.findById(id)
                .filter(h -> h.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> ApiException.notFound("Habit not found"));
    }

    public List<LocalDate> completionDates(Habit habit) {
        return completionRepository.findAllDates(habit);
    }

    public int startOffset(User user) {
        int dow = DateUtils.today(user).getDayOfWeek().getValue(); // 1..7 Mon..Sun
        return switch (user.getWeekStartDay().toUpperCase()) {
            case "SUNDAY" -> dow % 7;
            default -> dow - 1;
        };
    }

    private HabitResponse toResponse(Habit habit, LocalDate today, int offset) {
        return HabitResponse.from(habit, completionDates(habit), today, offset, today.lengthOfMonth());
    }
}