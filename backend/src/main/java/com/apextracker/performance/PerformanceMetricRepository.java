package com.apextracker.performance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apextracker.user.User;

public interface PerformanceMetricRepository extends JpaRepository<PerformanceMetric, Long> {

    Optional<PerformanceMetric> findByUserAndDate(User user, LocalDate date);

    List<PerformanceMetric> findByUserAndDateBetweenOrderByDateAsc(User user, LocalDate from, LocalDate to);

    Optional<PerformanceMetric> findTopByUserAndDateBeforeOrderByDateDesc(User user, LocalDate date);

    void deleteByUser(User user);
}