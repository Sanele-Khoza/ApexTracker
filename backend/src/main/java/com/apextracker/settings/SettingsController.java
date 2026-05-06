package com.apextracker.settings;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.apextracker.config.CurrentUserService;
import com.apextracker.user.User;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;
    private final CurrentUserService currentUser;

    public SettingsController(SettingsService settingsService, CurrentUserService currentUser) {
        this.settingsService = settingsService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public Map<String, Object> get() {
        User user = currentUser.get();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", user.getName());
        map.put("email", user.getEmail());
        map.put("timezone", user.getTimezone());
        map.put("weekStartDay", user.getWeekStartDay());
        map.put("bedTimeTarget", user.getBedTimeTarget());
        map.put("wakeTimeTarget", user.getWakeTimeTarget());
        map.put("weeklyStudyGoalMinutes", user.getWeeklyStudyGoalMinutes());
        map.put("quietHoursStart", user.getQuietHoursStart());
        map.put("quietHoursEnd", user.getQuietHoursEnd());
        map.put("theme", user.getTheme());
        map.put("emailNotifications", user.isEmailNotifications());
        map.put("reminderEnabled", user.isReminderEnabled());
        map.put("reminderDefaultMinutes", user.getReminderDefaultMinutes());
        return map;
    }

    @PutMapping
    public Map<String, Object> update(@RequestBody SettingsUpdateRequest req) {
        User updated = settingsService.update(currentUser.get(), req);
        // reserialize via get
        return get();
    }

    @GetMapping("/export")
    public Map<String, Object> export() {
        return settingsService.export(currentUser.get());
    }

    @DeleteMapping("/account")
    public void deleteAccount() {
        settingsService.deleteAccount(currentUser.get());
    }
}