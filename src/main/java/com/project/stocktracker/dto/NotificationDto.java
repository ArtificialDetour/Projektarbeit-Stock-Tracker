package com.project.stocktracker.dto;

/**
 * Response payload for one notification entry.
 */
public record NotificationDto(
        Long id,
        String title,
        String message,
        String icon,
        String iconStyle,
        boolean read,
        String timeLabel
) {}
