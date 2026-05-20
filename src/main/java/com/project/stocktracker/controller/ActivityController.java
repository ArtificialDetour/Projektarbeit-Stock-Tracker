package com.project.stocktracker.controller;

import com.project.stocktracker.dto.ActivityDto;
import com.project.stocktracker.entity.User;
import com.project.stocktracker.service.ActivityService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * REST endpoints for the user's recent activity feed.
 */
@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    /**
     * Returns recent activity entries for the authenticated user.
     */
    @GetMapping
    public List<ActivityDto> getActivities(@AuthenticationPrincipal User user) {
        if (user == null) return List.of();
        return activityService.getUserActivities(user);
    }

    /**
     * Records a CSV export activity for the authenticated user.
     */
    @PostMapping("/log-export")
    public ResponseEntity<Void> logExport(@AuthenticationPrincipal User user) {
        if (user != null) {
            activityService.logActivity(user, "EXPORT", "Data Exported", "You exported your portfolio and transaction history as CSV files.");
        }
        return ResponseEntity.ok().build();
    }
}
