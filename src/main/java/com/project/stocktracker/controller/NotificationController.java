package com.project.stocktracker.controller;

import com.project.stocktracker.dto.NotificationDto;
import com.project.stocktracker.entity.User;
import com.project.stocktracker.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for user notifications.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Returns notifications for the authenticated user.
     */
    @GetMapping
    public List<NotificationDto> getNotifications(@AuthenticationPrincipal User user) {
        if (user == null) return List.of();
        return notificationService.getNotifications(user);
    }

    /**
     * Returns the unread notification count.
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus(@AuthenticationPrincipal User user) {
        if (user == null) return Map.of("unreadCount", 0);
        return Map.of("unreadCount", notificationService.countUnread(user));
    }

    /**
     * Marks all notifications as read.
     */
    @PostMapping("/mark-read")
    public ResponseEntity<Map<String, Object>> markRead(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.ok(Map.of("unreadCount", 0));
        notificationService.markAllRead(user);
        return ResponseEntity.ok(Map.of("unreadCount", 0));
    }

    /**
     * Deletes all notifications for the authenticated user.
     */
    @DeleteMapping
    public ResponseEntity<Void> clearAll(@AuthenticationPrincipal User user) {
        if (user != null) {
            notificationService.deleteAll(user);
        }
        return ResponseEntity.ok().build();
    }
}
