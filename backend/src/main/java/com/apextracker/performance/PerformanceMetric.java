package com.apextracker.performance;

import java.time.Instant;
import java.time.LocalDate;

import com.apextracker.user.User;

import jakarta.persistence.*;

@Entity
@Table(name = "performance_metrics", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "date" }))
public class PerformanceMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private int productivityScore;

    @Column(nullable = false)
    private int studyScore;

    @Column(nullable = false)
    private int routineScore;

    @Column(nullable = false)
    private int sleepScore;

    @Column(nullable = false)
    private int activityScore;

    @Column(nullable = false)
    private int overallScore;

    @Column(length = 2000)
    private String details;

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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getProductivityScore() {
        return productivityScore;
    }

    public void setProductivityScore(int productivityScore) {
        this.productivityScore = productivityScore;
    }

    public int getStudyScore() {
        return studyScore;
    }

    public void setStudyScore(int studyScore) {
        this.studyScore = studyScore;
    }

    public int getRoutineScore() {
        return routineScore;
    }

    public void setRoutineScore(int routineScore) {
        this.routineScore = routineScore;
    }

    public int getSleepScore() {
        return sleepScore;
    }

    public void setSleepScore(int sleepScore) {
        this.sleepScore = sleepScore;
    }

    public int getActivityScore() {
        return activityScore;
    }

    public void setActivityScore(int activityScore) {
        this.activityScore = activityScore;
    }

    public int getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(int overallScore) {
        this.overallScore = overallScore;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}