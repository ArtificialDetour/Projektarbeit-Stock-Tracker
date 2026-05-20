package com.project.stocktracker.dto;

import com.project.stocktracker.entity.User;

/**
 * Result of a successful registration, including the one-time recovery code.
 */
public record RegistrationResultDto(
    User user, 
    String securityCode
    ) {
}
