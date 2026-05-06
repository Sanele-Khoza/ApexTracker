package com.apextracker.settings;

import java.util.Map;

import com.apextracker.user.User;

public record SettingsUpdateRequest(String name, String timezone, String weekStartDay, String bedTimeTarget,
        String wakeTimeTarget, Integer weeklyStudyGoalMinutes, String quietHoursStart, String quietHoursEnd,
        String theme, Boolean emailNotifications, Boolean reminderEnabled, Integer reminderDefaultMinutes) {
}