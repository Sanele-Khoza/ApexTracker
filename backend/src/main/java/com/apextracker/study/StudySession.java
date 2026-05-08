package com.apextracker.study;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Duration;

import com.apextracker.user.User;

import jakarta.persistence.*;

@Entity
@Table(name = "study_sessions")
public class StudySession {

    public enum State {
        ACTIVE, PAUSED, FINISHED
    }

    public enum Method {
        FOCUSED, POMODORO, REVIEW, PRACTICE, READING, WATCHING_LESSONS, OTHER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String subject;

    @Column
    private String topic;

    @Column(nullable = false)
    private Instant startTime;

    @Column
    private Instant endTime;

    @Column(nullable = false)
    private long accumulatedSeconds = 0;

    @Column
    private Instant lastResumeAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private State state = State.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Method method = Method.FOCUSED;

    @Column
    private Integer difficulty;

    @Column
    private Integer focusRating;

    @Column(length = 1000)
    private String notes;

    @Column
    private LocalDate date;

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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public long getAccumulatedSeconds() {
        return accumulatedSeconds;
    }

    public void setAccumulatedSeconds(long accumulatedSeconds) {
        this.accumulatedSeconds = accumulatedSeconds;
    }

    public Instant getLastResumeAt() {
        return lastResumeAt;
    }

    public void setLastResumeAt(Instant lastResumeAt) {
        this.lastResumeAt = lastResumeAt;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getFocusRating() {
        return focusRating;
    }

    public void setFocusRating(Integer focusRating) {
        this.focusRating = focusRating;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void addSegment(Instant now) {
        if (lastResumeAt != null) {
            accumulatedSeconds += Math.max(0, Duration.between(lastResumeAt, now).getSeconds());
        }
        lastResumeAt = null;
    }
}