package com.apextracker.review;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apextracker.user.User;

public interface DailyReviewRepository extends JpaRepository<DailyReview, Long> {

    Optional<DailyReview> findByUserAndDate(User user, LocalDate date);

    List<DailyReview> findByUserOrderByDateDesc(User user);

    Optional<DailyReview> findTopByUserAndDateBeforeOrderByDateDesc(User user, LocalDate date);

    void deleteByUser(User user);
}