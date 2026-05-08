package com.apextracker.study;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.apextracker.common.DateUtils;
import com.apextracker.config.CurrentUserService;
import com.apextracker.user.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/study")
public class StudyController {

    private final StudyService studyService;
    private final StudySessionRepository studyRepository;
    private final CurrentUserService currentUser;

    public StudyController(StudyService studyService, StudySessionRepository studyRepository,
            CurrentUserService currentUser) {
        this.studyService = studyService;
        this.studyRepository = studyRepository;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<StudySessionResponse> list() {
        User user = currentUser.get();
        return studyRepository.findByUserOrderByStartTimeDesc(user).stream()
                .map(s -> StudySessionResponse.from(s, DateUtils.zone(user)))
                .toList();
    }

    @PostMapping
    public StudySessionResponse start(@Valid @RequestBody StudyStartRequest req) {
        User user = currentUser.get();
        return StudySessionResponse.from(studyService.start(user, req), DateUtils.zone(user));
    }

    @PostMapping("/{id}/pause")
    public StudySessionResponse pause(@PathVariable Long id) {
        User user = currentUser.get();
        return StudySessionResponse.from(studyService.pause(id, user), DateUtils.zone(user));
    }

    @PostMapping("/{id}/resume")
    public StudySessionResponse resume(@PathVariable Long id) {
        User user = currentUser.get();
        return StudySessionResponse.from(studyService.resume(id, user), DateUtils.zone(user));
    }

    @PostMapping("/{id}/finish")
    public StudySessionResponse finish(@PathVariable Long id) {
        User user = currentUser.get();
        return StudySessionResponse.from(studyService.finish(id, user), DateUtils.zone(user));
    }

    @PutMapping("/{id}")
    public StudySessionResponse update(@PathVariable Long id, @RequestBody StudyUpdateRequest req) {
        User user = currentUser.get();
        return StudySessionResponse.from(studyService.update(id, user, req), DateUtils.zone(user));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        studyService.delete(id, currentUser.get());
    }
}