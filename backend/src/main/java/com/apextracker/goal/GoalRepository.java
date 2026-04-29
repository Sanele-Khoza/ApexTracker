package com.apextracker.goal;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apextracker.user.User;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findByUserOrderByEndDateAsc(User user);

    List<Goal> findByUserAndStatus(User user, Goal.Status status);

    void deleteByUser(User user);
}