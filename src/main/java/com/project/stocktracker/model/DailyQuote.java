package com.project.stocktracker.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents one OHLCV quote point from a market data provider.
 */
public record DailyQuote(
        LocalDateTime date,
        BigDecimal open,
        BigDecimal close,
        BigDecimal high,
        BigDecimal low,
        Long volume
) {}
