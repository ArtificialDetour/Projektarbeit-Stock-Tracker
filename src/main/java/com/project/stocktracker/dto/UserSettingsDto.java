package com.project.stocktracker.dto;

/**
 * Partial settings update payload for user preferences.
 */
public record UserSettingsDto(
    Boolean darkMode,
    Boolean priceAlerts,
    Boolean transactionUpdates
) {}
