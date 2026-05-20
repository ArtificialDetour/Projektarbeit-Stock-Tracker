package com.project.stocktracker.dto;

/**
 * Profile and settings payload for authenticated users.
 */
public record UserProfileResponseDto(
    String email,
    String firstName,
    String lastName,
    String displayName,
    boolean darkMode,
    boolean priceAlerts,
    boolean transactionUpdates
) {}
