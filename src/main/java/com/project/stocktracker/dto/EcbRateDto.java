package com.project.stocktracker.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response payload for the latest ECB benchmark rate.
 */
public record EcbRateDto(
        BigDecimal rate,
        LocalDateTime lastUpdated
) {}
