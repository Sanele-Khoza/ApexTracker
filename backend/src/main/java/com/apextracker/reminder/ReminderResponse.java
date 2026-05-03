package com.apextracker.reminder;

import java.time.Instant;

public record ReminderResponse(Long id, String type, String message, Instant scheduledAt, boolean read,
        Long relatedId, String entityType, Instant createdAt) {

    public static ReminderResponse from(Reminder r) {
        return new ReminderResponse(r.getId(), r.getType().name(), r.getMessage(), r.getScheduledAt(), r.isRead(),
                r.getRelatedId(), r.getEntityType(), r.getCreatedAt());
    }
}