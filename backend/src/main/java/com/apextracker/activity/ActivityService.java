package com.apextracker.activity;

import java.time.Instant;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apextracker.common.ApiException;
import com.apextracker.common.DateUtils;
import com.apextracker.user.User;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;

    public ActivityService(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    public Activity get(Long id, User user) {
        return activityRepository.findById(id)
                .filter(a -> a.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> ApiException.notFound("Activity not found"));
    }

    @Transactional
    public Activity start(User user, ActivityRequest req) {
        activityRepository.findByUserAndActiveTrue(user).forEach(a -> {
            // Any previously running activity must be stopped first
            stopInternal(a, Instant.now());
        });
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setName(req.name().trim());
        activity.setCategory(req.category());
        activity.setNotes(req.notes());
        activity.setStartTime(Instant.now());
        activity.setActive(true);
        return activityRepository.save(activity);
    }

    @Transactional
    public Activity stop(Long id, User user) {
        Activity activity = get(id, user);
        if (!activity.isActive()) {
            throw ApiException.badRequest("This activity is already stopped");
        }
        return stopInternal(activity, Instant.now());
    }

    @Transactional
    public Activity stopActive(User user) {
        return activityRepository.findByUserAndActiveTrue(user).stream()
                .findFirst()
                .map(a -> stopInternal(a, Instant.now()))
                .orElseThrow(() -> ApiException.badRequest("No active activity to stop"));
    }

    @Transactional
    public Activity update(Long id, User user, ActivityRequest req) {
        Activity activity = get(id, user);
        if (req.name() != null)
            activity.setName(req.name().trim());
        if (req.category() != null)
            activity.setCategory(req.category());
        if (req.notes() != null)
            activity.setNotes(req.notes());
        if (req.productivityRating() != null)
            activity.setProductivityRating(req.productivityRating());
        return activityRepository.save(activity);
    }

    @Transactional
    public void delete(Long id, User user) {
        Activity activity = get(id, user);
        activityRepository.delete(activity);
    }

    private Activity stopInternal(Activity activity, Instant now) {
        long minutes = Math.max(1, java.time.Duration.between(activity.getStartTime(), now).toMinutes());
        activity.setEndTime(now);
        activity.setDurationMinutes((int) minutes);
        activity.setActive(false);
        return activityRepository.save(activity);
    }

    public ZoneId zone(Activity activity, User user) {
        return DateUtils.zone(user);
    }
}