package com.project.stocktracker.dto;

/**
 * Response payload for one recent activity entry.
 */
public record ActivityDto(
        Long id,
        String title,
        String description,
        String type,
        String timeLabel
) {}
