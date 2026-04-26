package com.apextracker.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.apextracker.user.User;

public final class DateUtils {

    private DateUtils() {
    }

    public static ZoneId zone(User user) {
        try {
            return ZoneId.of(user.getTimezone());
        } catch (Exception e) {
            return ZoneId.systemDefault();
        }
    }

    public static LocalDate today(User user) {
        return LocalDate.now(zone(user));
    }

    public static LocalDate dayWithOffset(User user, int days) {
        return today(user).plusDays(days);
    }

    public static Instant dayStart(LocalDate day, ZoneId zone) {
        return day.atStartOfDay(zone).toInstant();
    }

    public static Instant dayEnd(LocalDate day, ZoneId zone) {
        return day.plusDays(1).atStartOfDay(zone).toInstant();
    }

    public static Instant atTime(LocalDate day, LocalTime time, ZoneId zone) {
        return day.atTime(time).atZone(zone).toInstant();
    }

    public static Instant atTime(LocalDate day, String time, ZoneId zone) {
        return atTime(day, parseTime(time), zone);
    }

    public static LocalTime parseTime(String time) {
        if (time == null || time.isBlank()) {
            throw new ApiException(400, "A valid time is required");
        }
        return LocalTime.parse(time.trim(), DateTimeFormatter.ofPattern("H:mm"));
    }

    public static String formatDuration(long minutes) {
        long h = minutes / 60;
        long m = minutes % 60;
        if (h == 0) {
            return m + "m";
        }
        return h + "h " + m + "m";
    }
}