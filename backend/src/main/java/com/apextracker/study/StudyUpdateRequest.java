package com.apextracker.study;

public record StudyUpdateRequest(Integer focusRating, Integer difficulty, String notes, String topic) {
}