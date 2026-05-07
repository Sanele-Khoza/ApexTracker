package com.apextracker.sleep;

import java.time.LocalDate;

public record SleepResponse(Long id, LocalDate date, java.time.LocalTime bedTime, java.time.LocalTime sleepTime,
        java.time.LocalTime wakeTime, Integer durationMinutes, Integer quality, String notes,
        Integer deltaBedMinutes, Integer deltaWakeMinutes, String targetBedTime, String targetWakeTime) {

    public static SleepResponse from(SleepRecord s, String targetBed, String targetWake) {
        Integer deltaBed = null;
        Integer deltaWake = null;
        try {
            if (s.getBedTime() != null) {
                deltaBed = deltaMinutes(s.getBedTime(), java.time.LocalTime.parse(targetBed));
            }
            deltaWake = deltaMinutes(s.getWakeTime(), java.time.LocalTime.parse(targetWake));
        } catch (Exception ignored) {
        }
        return new SleepResponse(s.getId(), s.getDate(), s.getBedTime(), s.getSleepTime(), s.getWakeTime(),
                s.getDurationMinutes(), s.getQuality(), s.getNotes(), deltaBed, deltaWake, targetBed, targetWake);
    }

    private static int deltaMinutes(java.time.LocalTime actual, java.time.LocalTime target) {
        return actual.getHour() * 60 + actual.getMinute() - (target.getHour() * 60 + target.getMinute());
    }
}