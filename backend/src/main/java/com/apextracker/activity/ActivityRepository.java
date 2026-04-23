package com.apextracker.activity;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.apextracker.user.User;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByUserOrderByStartTimeDesc(User user);

    List<Activity> findByUserAndActiveTrue(User user);

    List<Activity> findByUserAndStartTimeBetween(User user, Instant start, Instant end);

    @Query("select coalesce(sum(a.durationMinutes), 0) from Activity a where a.user = :user and a.category = :category and a.startTime between :start and :end")
    long sumDurationByCategoryBetween(@Param("user") User user, @Param("category") Activity.Category category,
            @Param("start") Instant start, @Param("end") Instant end);

    void deleteByUser(User user);
}