package com.apextracker.task;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.apextracker.user.User;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUserOrderByDueDateAscDueTimeAsc(User user);

    List<Task> findByUserAndStatus(User user, Task.Status status);

    List<Task> findByUserAndDueDate(User user, LocalDate dueDate);

    List<Task> findByUserAndDueDateBetween(User user, LocalDate start, LocalDate end);

    List<Task> findByUserAndCategory(User user, Task.Category category);

    long countByUserAndStatus(User user, Task.Status status);

    long countByUser(User user);

    long countByUserAndMissedTrue(User user);

    long countByUserAndCompletedAtBetween(User user, java.time.Instant start, java.time.Instant end);

    @Query("select t from Task t where t.user = :user and t.status in :statuses order by t.dueDate asc nulls last, t.dueTime asc nulls last")
    List<Task> findByUserAndStatusIn(@Param("user") User user, @Param("statuses") List<Task.Status> statuses);

    @Query("select t.category, count(t) from Task t where t.user = :user and t.missed = true group by t.category order by count(t) desc")
    List<Object[]> countMissedByCategory(@Param("user") User user);

    List<Task> findByUserAndRecurrenceNot(User user, Task.Recurrence recurrence);

    @Query("select t from Task t where t.user.id = :userId and t.status in :statuses and t.dueDate is not null and t.dueDate <= :today")
    List<Task> findOverdueCandidates(@Param("userId") Long userId, @Param("statuses") List<Task.Status> statuses,
            @Param("today") java.time.LocalDate today);

    void deleteByUser(User user);
}