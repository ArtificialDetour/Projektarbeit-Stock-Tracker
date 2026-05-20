package com.project.stocktracker.service;

import com.project.stocktracker.dto.ActivityDto;
import com.project.stocktracker.entity.Activity;
import com.project.stocktracker.entity.User;
import com.project.stocktracker.repository.ActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Manages the user's recent activity feed.
 */
@Service
public class ActivityService {

    private final ActivityRepository activityRepository;

    /**
     * Creates an activity service backed by the activity repository.
     */
    public ActivityService(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    /**
     * Stores a user-facing activity entry and trims the feed to the latest entries.
     */
    @Transactional
    public void logActivity(User user, String type, String title, String description) {
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setType(type);
        activity.setTitle(title);
        activity.setDescription(description);
        activityRepository.save(activity);

        // Keep the activity popover compact without requiring pagination.
        List<Activity> activities = activityRepository.findByUserOrderByCreatedAtDesc(user);
        if (activities.size() > 20) {
            List<Activity> toDelete = activities.subList(20, activities.size());
            activityRepository.deleteAll(toDelete);
        }
    }

    /**
     * Returns the latest activity entries for the given user.
     */
    public List<ActivityDto> getUserActivities(User user) {
        return activityRepository.findTop20ByUserOrderByCreatedAtDescIdDesc(user)
                .stream().map(this::toDto).toList();
    }

    /**
     * Removes all activity entries for the given user.
     */
    public void clearActivities(User user) {
        activityRepository.deleteByUser(user);
    }

    /**
     * Converts an activity entity into the dashboard response shape.
     */
    private ActivityDto toDto(Activity a) {
        return new ActivityDto(a.getId(), a.getTitle(), a.getDescription(), a.getType(), timeLabel(a.getCreatedAt()));
    }

    /**
     * Formats an activity timestamp as a compact relative label.
     */
    private String timeLabel(LocalDateTime t) {
        if (t == null) return "";
        long minutes = ChronoUnit.MINUTES.between(t, LocalDateTime.now());
        if (minutes < 2)  return "Just Now";
        if (minutes < 60) return minutes + "m ago";
        long hours = ChronoUnit.HOURS.between(t, LocalDateTime.now());
        if (hours < 24)   return hours + "h ago";
        return ChronoUnit.DAYS.between(t, LocalDateTime.now()) + "d ago";
    }
}
