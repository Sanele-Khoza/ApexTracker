package com.apextracker.goal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GoalRequest(
        @NotBlank(message = "Goal name is required") String name,
        Goal.Category category,
        @NotNull(message = "Target value is required") @Positive(message = "Target must be positive") Double targetValue,
        String unit,
        @NotBlank(message = "Start date is required") String startDate,
        @NotBlank(message = "End date is required") String endDate,
        Double manualProgress) {

    public GoalRequest {
        if (category == null)
            category = Goal.Category.STUDY;
        if (unit == null || unit.isBlank())
            unit = "minutes";
    }
}