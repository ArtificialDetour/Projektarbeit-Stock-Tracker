package com.project.stocktracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Portfolio holding view with current value and performance metrics.
 */
public record HoldingDto(
        String symbol,
        String assetName,
        BigDecimal quantity,
        BigDecimal avgCostBasis,
        BigDecimal costBasisTotal,
        BigDecimal currentPrice,
        BigDecimal currentValue,
        BigDecimal performancePercent,
        BigDecimal weeklyChangePercent,
        LocalDate firstPurchaseDate
) {}
