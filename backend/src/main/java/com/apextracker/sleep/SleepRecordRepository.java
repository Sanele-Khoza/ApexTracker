package com.apextracker.sleep;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.apextracker.user.User;

public interface SleepRecordRepository extends JpaRepository<SleepRecord, Long> {

    List<SleepRecord> findByUserOrderByDateDesc(User user);

    List<SleepRecord> findByUserAndDateBetween(User user, LocalDate from, LocalDate to);

    Optional<SleepRecord> findByUserAndDate(User user, LocalDate date);

    void deleteByUser(User user);

    @Query("select coalesce(avg(s.durationMinutes), 0) from SleepRecord s where s.user = :user and s.durationMinutes is not null and s.date between :from and :to")
    double averageDuration(@Param("user") User user, @Param("from") LocalDate from, @Param("to") LocalDate to);
}