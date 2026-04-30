package com.apextracker.habit;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HabitCompletionRepository extends JpaRepository<HabitCompletion, Long> {

    Optional<HabitCompletion> findByHabitAndDate(Habit habit, LocalDate date);

    boolean existsByHabitAndDate(Habit habit, LocalDate date);

    long countByHabitAndDateBetween(Habit habit, LocalDate from, LocalDate to);

    @Query("select hc.date from HabitCompletion hc where hc.habit = :habit order by hc.date asc")
    List<LocalDate> findAllDates(@Param("habit") Habit habit);

    List<HabitCompletion> findByHabitInAndDateBetween(List<Habit> habits, LocalDate from, LocalDate to);

    @Query("select count(hc) from HabitCompletion hc where hc.habit.user = :user and hc.date between :from and :to")
    long countByUserAndDateBetween(@Param("user") com.apextracker.user.User user, @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    void deleteByHabit(Habit habit);
}