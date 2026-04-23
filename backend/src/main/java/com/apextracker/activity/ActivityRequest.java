package com.apextracker.activity;

import jakarta.validation.constraints.NotBlank;

public record ActivityRequest(
        @NotBlank(message = "Activity name is required") String name,
        Activity.Category category,
        String notes,
        Integer productivityRating) {

    public ActivityRequest {
        if (category == null)
            category = Activity.Category.OTHER;
    }
}