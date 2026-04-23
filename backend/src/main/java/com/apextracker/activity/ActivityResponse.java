package com.apextracker.activity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public record ActivityResponse(Long id, String name, String category, Instant startTime, Instant endTime,
        Integer durationMinutes, String notes, Integer productivityRating, boolean active, LocalDate date,
        Instant createdAt) {

    public static ActivityResponse from(Activity a, ZoneId zone) {
        LocalDate date = a.getStartTime() != null
                ? java.time.LocalDate.ofInstant(a.getStartTime(), zone)
                : null;
        return new ActivityResponse(a.getId(), a.getName(), a.getCategory().name(), a.getStartTime(), a.getEndTime(),
                a.getDurationMinutes(), a.getNotes(), a.getProductivityRating(), a.isActive(), date, a.getCreatedAt());
    }
}