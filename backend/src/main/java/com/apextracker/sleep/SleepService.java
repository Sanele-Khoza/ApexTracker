package com.apextracker.sleep;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apextracker.common.ApiException;
import com.apextracker.user.User;

@Service
public class SleepService {

    private final SleepRecordRepository sleepRepository;

    public SleepService(SleepRecordRepository sleepRepository) {
        this.sleepRepository = sleepRepository;
    }

    public SleepRecord get(Long id, User user) {
        return sleepRepository.findById(id)
                .filter(s -> s.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> ApiException.notFound("Sleep record not found"));
    }

    @Transactional
    public SleepRecord create(User user, SleepRequest req) {
        LocalDate date = LocalDate.parse(req.date());
        sleepRepository.findByUserAndDate(user, date).ifPresent(existing -> {
            throw ApiException.conflict("A sleep record for this date already exists");
        });
        return persist(user, new SleepRecord(), req);
    }

    @Transactional
    public SleepRecord update(Long id, User user, SleepRequest req) {
        SleepRecord record = get(id, user);
        if (req.date() != null) {
            record.setDate(LocalDate.parse(req.date()));
        }
        return persist(user, record, req);
    }

    @Transactional
    public void delete(Long id, User user) {
        SleepRecord record = get(id, user);
        sleepRepository.delete(record);
    }

    private SleepRecord persist(User user, SleepRecord record, SleepRequest req) {
        record.setUser(user);
        if (req.date() != null)
            record.setDate(LocalDate.parse(req.date()));
        if (req.bedTime() != null)
            record.setBedTime(com.apextracker.common.DateUtils.parseTime(req.bedTime()));
        if (req.sleepTime() != null)
            record.setSleepTime(com.apextracker.common.DateUtils.parseTime(req.sleepTime()));
        if (req.wakeTime() != null)
            record.setWakeTime(com.apextracker.common.DateUtils.parseTime(req.wakeTime()));
        if (req.quality() != null)
            record.setQuality(req.quality());
        if (req.notes() != null)
            record.setNotes(req.notes());
        record.setDurationMinutes(record.computeDuration());
        if (record.getDurationMinutes() == null) {
            throw ApiException.badRequest("Wake-up time is required together with a bed or sleep time");
        }
        return sleepRepository.save(record);
    }
}