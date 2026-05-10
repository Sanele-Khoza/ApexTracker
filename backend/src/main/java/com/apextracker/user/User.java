package com.apextracker.user;

import java.time.Instant;
import java.time.ZoneId;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 190)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String timezone = ZoneId.systemDefault().toString();

    @Column(nullable = false)
    private String weekStartDay = "MONDAY";

    @Column(nullable = false)
    private String bedTimeTarget = "22:30";

    @Column(nullable = false)
    private String wakeTimeTarget = "06:30";

    @Column(nullable = false)
    private int weeklyStudyGoalMinutes = 900;

    @Column(nullable = false)
    private String quietHoursStart = "22:00";

    @Column(nullable = false)
    private String quietHoursEnd = "07:00";

    @Column(nullable = false)
    private String theme = "dark";

    @Column(nullable = false)
    private int reminderDefaultMinutes = 15;

    @Column(nullable = false)
    private boolean emailNotifications = true;

    @Column(nullable = false)
    private boolean reminderEnabled = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getWeekStartDay() {
        return weekStartDay;
    }

    public void setWeekStartDay(String weekStartDay) {
        this.weekStartDay = weekStartDay;
    }

    public String getBedTimeTarget() {
        return bedTimeTarget;
    }

    public void setBedTimeTarget(String bedTimeTarget) {
        this.bedTimeTarget = bedTimeTarget;
    }

    public String getWakeTimeTarget() {
        return wakeTimeTarget;
    }

    public void setWakeTimeTarget(String wakeTimeTarget) {
        this.wakeTimeTarget = wakeTimeTarget;
    }

    public int getWeeklyStudyGoalMinutes() {
        return weeklyStudyGoalMinutes;
    }

    public void setWeeklyStudyGoalMinutes(int weeklyStudyGoalMinutes) {
        this.weeklyStudyGoalMinutes = weeklyStudyGoalMinutes;
    }

    public String getQuietHoursStart() {
        return quietHoursStart;
    }

    public void setQuietHoursStart(String quietHoursStart) {
        this.quietHoursStart = quietHoursStart;
    }

    public String getQuietHoursEnd() {
        return quietHoursEnd;
    }

    public void setQuietHoursEnd(String quietHoursEnd) {
        this.quietHoursEnd = quietHoursEnd;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public int getReminderDefaultMinutes() {
        return reminderDefaultMinutes;
    }

    public void setReminderDefaultMinutes(int reminderDefaultMinutes) {
        this.reminderDefaultMinutes = reminderDefaultMinutes;
    }

    public boolean isEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public boolean isReminderEnabled() {
        return reminderEnabled;
    }

    public void setReminderEnabled(boolean reminderEnabled) {
        this.reminderEnabled = reminderEnabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}