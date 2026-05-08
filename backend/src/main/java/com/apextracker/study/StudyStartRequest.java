package com.apextracker.study;

import jakarta.validation.constraints.NotBlank;

public record StudyStartRequest(
        @NotBlank(message = "Subject is required") String subject,
        String topic,
        StudySession.Method method,
        Integer difficulty,
        String notes) {

    public StudyStartRequest {
        if (method == null)
            method = StudySession.Method.FOCUSED;
    }
}