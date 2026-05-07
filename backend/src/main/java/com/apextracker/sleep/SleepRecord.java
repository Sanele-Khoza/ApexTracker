package com.apextracker.sleep;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import com.apextracker.user.User;

import jakarta.persistence.*;

@Entity
@Table(name = "sleep_records")
public class SleepRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column
    private LocalTime bedTime;

    @Column
    private LocalTime sleepTime;

    @Column(nullable = false)
    private LocalTime wakeTime;

    @Column
    private Integer durationMinutes;

    @Column
    private Integer quality;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Integer computeDuration() {
        if (wakeTime == null)
            return null;
        LocalTime start = sleepTime != null ? sleepTime : bedTime;
        if (start == null)
            return null;
        int startMin = start.getHour() * 60 + start.getMinute();
        int wakeMin = wakeTime.getHour() * 60 + wakeTime.getMinute();
        int duration = startMin < wakeMin ? wakeMin - startMin : (1440 - startMin) + wakeMin;
        return duration == 0 ? 1440 : duration;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getBedTime() {
        return bedTime;
    }

    public void setBedTime(LocalTime bedTime) {
        this.bedTime = bedTime;
    }

    public LocalTime getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(LocalTime sleepTime) {
        this.sleepTime = sleepTime;
    }

    public LocalTime getWakeTime() {
        return wakeTime;
    }

    public void setWakeTime(LocalTime wakeTime) {
        this.wakeTime = wakeTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getQuality() {
        return quality;
    }

    public void setQuality(Integer quality) {
        this.quality = quality;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}