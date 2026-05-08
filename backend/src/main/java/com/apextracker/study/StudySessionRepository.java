package com.apextracker.study;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.apextracker.user.User;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    List<StudySession> findByUserOrderByStartTimeDesc(User user);

    List<StudySession> findByUserAndState(User user, StudySession.State state);

    List<StudySession> findByUserAndStartTimeBetween(User user, Instant start, Instant end);

    @Query("select coalesce(sum(s.accumulatedSeconds), 0) from StudySession s where s.user = :user and s.date >= :from and s.date <= :to")
    long sumSecondsBetweenDates(@Param("user") User user, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select s.subject, coalesce(sum(s.accumulatedSeconds), 0) from StudySession s where s.user = :user and s.date >= :from and s.date <= :to group by s.subject")
    List<Object[]> sumSecondsBySubject(@Param("user") User user, @Param("from") LocalDate from, @Param("to") LocalDate to);

    long countByUser(User user);

    void deleteByUser(User user);
}