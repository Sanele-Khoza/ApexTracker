package com.apextracker.habit;

import java.time.Instant;
import java.time.LocalDate;

import com.apextracker.user.User;

import jakarta.persistence.*;

@Entity
@Table(name = "habits")
public class Habit {

    public enum Frequency {
        DAILY, WEEKLY, MONTHLY
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Frequency frequency = Frequency.DAILY;

    @Column(nullable = false)
    private int target = 1;

    @Column
    private Integer reminderMinutes;

    @Column
    private LocalDate reminderSentDate;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public void setFrequency(Frequency frequency) {
        this.frequency = frequency;
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public Integer getReminderMinutes() {
        return reminderMinutes;
    }

    public void setReminderMinutes(Integer reminderMinutes) {
        this.reminderMinutes = reminderMinutes;
    }

    public LocalDate getReminderSentDate() {
        return reminderSentDate;
    }

    public void setReminderSentDate(LocalDate reminderSentDate) {
        this.reminderSentDate = reminderSentDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}