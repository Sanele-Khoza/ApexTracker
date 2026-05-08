package com.apextracker.study;

import java.time.LocalDate;
import java.time.ZoneId;

public record StudySessionResponse(Long id, String subject, String topic, java.time.Instant startTime,
        java.time.Instant endTime, long durationSeconds, String state, String method, Integer difficulty,
        Integer focusRating, String notes, LocalDate date) {

    public static StudySessionResponse from(StudySession s, ZoneId zone) {
        LocalDate date = s.getDate() != null ? s.getDate()
                : s.getStartTime() != null ? LocalDate.ofInstant(s.getStartTime(), zone) : null;
        return new StudySessionResponse(s.getId(), s.getSubject(), s.getTopic(), s.getStartTime(), s.getEndTime(),
                s.getAccumulatedSeconds(), s.getState().name(), s.getMethod().name(), s.getDifficulty(),
                s.getFocusRating(), s.getNotes(), date);
    }
}