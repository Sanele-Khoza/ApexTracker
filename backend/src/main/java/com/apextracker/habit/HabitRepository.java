package com.apextracker.habit;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apextracker.user.User;

public interface HabitRepository extends JpaRepository<Habit, Long> {

    List<Habit> findByUserOrderByCreatedAtAsc(User user);

    void deleteByUser(User user);
}