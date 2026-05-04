package com.apextracker.review;

import java.time.Instant;
import java.time.LocalDate;

public record DailyReviewResponse(Long id, LocalDate date, String summary, String reflection, Instant updatedAt) {

    public static DailyReviewResponse from(DailyReview review) {
        return new DailyReviewResponse(review.getId(), review.getDate(), review.getSummary(), review.getReflection(),
                review.getUpdatedAt());
    }
}