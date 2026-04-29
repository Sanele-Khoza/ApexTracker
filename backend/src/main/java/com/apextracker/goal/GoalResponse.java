package com.apextracker.goal;

import java.time.LocalDate;

public record GoalResponse(Long id, String name, String category, double targetValue, String unit, double progressValue,
        double progressPct, LocalDate startDate, LocalDate endDate, String status, String progressLabel,
        String targetLabel, int daysLeft) {

    public static GoalResponse from(Goal goal, double progress, String progressLabel, String targetLabel) {
        double pct = goal.getTargetValue() <= 0 ? 0 : Math.round((progress / goal.getTargetValue()) * 10000) / 100.0;
        LocalDate today = LocalDate.now();
        return new GoalResponse(goal.getId(), goal.getName(), goal.getCategory().name(), goal.getTargetValue(),
                goal.getUnit(), progress, pct, goal.getStartDate(), goal.getEndDate(), goal.getStatus().name(),
                progressLabel, targetLabel,
                goal.getEndDate() != null ? (int) java.time.temporal.ChronoUnit.DAYS.between(today, goal.getEndDate()) : 0);
    }
}