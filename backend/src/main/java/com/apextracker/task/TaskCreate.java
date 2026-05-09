package com.apextracker.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TaskCreate(
        @NotBlank(message = "Task name is required") String name,
        String description,
        Task.Category category,
        Task.Priority priority,
        String dueDate,
        String dueTime,
        Integer estimatedMinutes,
        Task.Recurrence recurrence,
        Integer reminderMinutes) {

    public TaskCreate {
        if (category == null)
            category = Task.Category.OTHER;
        if (priority == null)
            priority = Task.Priority.MEDIUM;
        if (recurrence == null)
            recurrence = Task.Recurrence.NONE;
    }
}