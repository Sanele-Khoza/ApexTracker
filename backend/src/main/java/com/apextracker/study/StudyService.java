package com.apextracker.study;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apextracker.common.ApiException;
import com.apextracker.common.DateUtils;
import com.apextracker.reminder.Reminder;
import com.apextracker.reminder.ReminderService;
import com.apextracker.user.User;

@Service
public class StudyService {

    private final StudySessionRepository studyRepository;
    private final ReminderService reminderService;

    public StudyService(StudySessionRepository studyRepository, ReminderService reminderService) {
        this.studyRepository = studyRepository;
        this.reminderService = reminderService;
    }

    public StudySession get(Long id, User user) {
        return studyRepository.findById(id)
                .filter(s -> s.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> ApiException.notFound("Study session not found"));
    }

    @Transactional
    public StudySession start(User user, StudyStartRequest req) {
        studyRepository.findByUserAndState(user, StudySession.State.ACTIVE).forEach(s -> {
            // auto-finish any active session
            s.addSegment(Instant.now());
            s.setEndTime(Instant.now());
            s.setState(StudySession.State.FINISHED);
            s.setDate(DateUtils.today(user));
            studyRepository.save(s);
        });
        StudySession session = new StudySession();
        session.setUser(user);
        session.setSubject(req.subject().trim());
        session.setTopic(req.topic());
        session.setMethod(req.method());
        session.setDifficulty(req.difficulty());
        session.setNotes(req.notes());
        session.setStartTime(Instant.now());
        session.setLastResumeAt(Instant.now());
        session.setState(StudySession.State.ACTIVE);
        session.setDate(DateUtils.today(user));
        return studyRepository.save(session);
    }

    @Transactional
    public StudySession pause(Long id, User user) {
        StudySession session = get(id, user);
        if (session.getState() != StudySession.State.ACTIVE) {
            throw ApiException.badRequest("Session is not active");
        }
        session.addSegment(Instant.now());
        session.setLastResumeAt(null);
        session.setState(StudySession.State.PAUSED);
        return studyRepository.save(session);
    }

    @Transactional
    public StudySession resume(Long id, User user) {
        StudySession session = get(id, user);
        if (session.getState() != StudySession.State.PAUSED) {
            throw ApiException.badRequest("Session is not paused");
        }
        session.setLastResumeAt(Instant.now());
        session.setState(StudySession.State.ACTIVE);
        return studyRepository.save(session);
    }

    @Transactional
    public StudySession finish(Long id, User user) {
        StudySession session = get(id, user);
        if (session.getState() == StudySession.State.FINISHED) {
            throw ApiException.badRequest("Session is already finished");
        }
        if (session.getState() == StudySession.State.ACTIVE) {
            session.addSegment(Instant.now());
        }
        session.setEndTime(Instant.now());
        session.setLastResumeAt(null);
        session.setState(StudySession.State.FINISHED);
        session.setDate(DateUtils.today(user));

        long minutes = Math.max(1, session.getAccumulatedSeconds() / 60);
        int target = user.getWeeklyStudyGoalMinutes();
        reminderService.create(user, Reminder.Type.STUDY,
                "Study session finished: " + minutes + " min on " + session.getSubject() + ". Daily study total updated.",
                Instant.now(), session.getId(), "STUDY");
        return studyRepository.save(session);
    }

    @Transactional
    public StudySession update(Long id, User user, StudyUpdateRequest req) {
        StudySession session = get(id, user);
        if (req.focusRating() != null)
            session.setFocusRating(req.focusRating());
        if (req.difficulty() != null)
            session.setDifficulty(req.difficulty());
        if (req.notes() != null)
            session.setNotes(req.notes());
        if (req.topic() != null)
            session.setTopic(req.topic());
        return studyRepository.save(session);
    }

    @Transactional
    public void delete(Long id, User user) {
        StudySession session = get(id, user);
        studyRepository.delete(session);
    }
}