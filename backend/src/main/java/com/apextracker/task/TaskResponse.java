package com.apextracker.task;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record TaskResponse(
        Long id,
        String name,
        String description,
        String category,
        String priority,
        LocalDate dueDate,
        LocalTime dueTime,
        Integer estimatedMinutes,
        String recurrence,
        Integer reminderMinutes,
        String status,
        Instant completedAt,
        boolean missed,
        int rescheduleCount,
        Instant createdAt,
        Instant updatedAt) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getName(),
                task.getDescription(),
                task.getCategory().name(),
                task.getPriority().name(),
                task.getDueDate(),
                task.getDueTime(),
                task.getEstimatedMinutes(),
                task.getRecurrence().name(),
                task.getReminderMinutes(),
                task.getStatus().name(),
                task.getCompletedAt(),
                task.isMissed(),
                task.getRescheduleCount(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}