package com.apextracker.habit;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

public record HabitRequest(
        @NotBlank(message = "Habit name is required") String name,
        Habit.Frequency frequency,
        Integer target,
        Integer reminderMinutes) {

    public HabitRequest {
        if (frequency == null)
            frequency = Habit.Frequency.DAILY;
        if (target == null || target < 1)
            target = 1;
    }
}