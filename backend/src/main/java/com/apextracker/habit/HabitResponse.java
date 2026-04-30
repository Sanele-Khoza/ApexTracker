package com.apextracker.habit;

import java.time.LocalDate;
import java.util.List;

public record HabitResponse(Long id, String name, String frequency, int target, Integer reminderMinutes,
        LocalDate lastCompleted, int currentStreak, int longestStreak, long completedThisWeek, long weekTarget,
        double weekProgress, boolean completedToday, long completedThisMonth, double monthProgress) {

    public static HabitResponse from(Habit habit, List<LocalDate> dates, LocalDate today, int startOfWeekOffset,
            int daysInMonth) {
        LocalDate weekStart = today.minusDays(startOfWeekOffset);
        LocalDate monthStart = today.withDayOfMonth(1);
        long thisWeek = dates.stream().filter(d -> !d.isBefore(weekStart) && !d.isAfter(today)).count();
        long thisMonth = dates.stream().filter(d -> !d.isBefore(monthStart) && !d.isAfter(today)).count();
        double weekTarget = habit.getFrequency() == Habit.Frequency.DAILY
                ? habit.getTarget() * (startOfWeekOffset + 1L)
                : habit.getTarget();
        double weekProgress = weekTarget == 0 ? 0 : Math.round((thisWeek / weekTarget) * 100) / 100.0;
        double monthTarget = habit.getFrequency() == Habit.Frequency.DAILY
                ? habit.getTarget() * (long) today.getDayOfMonth()
                : (habit.getFrequency() == Habit.Frequency.WEEKLY ? habit.getTarget() * 4 : habit.getTarget());
        double monthProgress = monthTarget == 0 ? 0 : Math.round((thisMonth / monthTarget) * 100) / 100.0;
        return new HabitResponse(habit.getId(), habit.getName(), habit.getFrequency().name(), habit.getTarget(),
                habit.getReminderMinutes(),
                dates.isEmpty() ? null : dates.get(dates.size() - 1),
                computeCurrentStreak(dates, today),
                computeLongestStreak(dates),
                thisWeek, (long) weekTarget, weekProgress,
                dates.contains(today),
                thisMonth, monthProgress);
    }

    private static int computeCurrentStreak(List<LocalDate> dates, LocalDate today) {
        if (!dates.contains(today)) {
            return 0;
        }
        int streak = 0;
        LocalDate cursor = today;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private static int computeLongestStreak(List<LocalDate> dates) {
        if (dates.isEmpty())
            return 0;
        int longest = 1;
        int current = 1;
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i).minusDays(1).equals(dates.get(i - 1))) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 1;
            }
        }
        return longest;
    }
}