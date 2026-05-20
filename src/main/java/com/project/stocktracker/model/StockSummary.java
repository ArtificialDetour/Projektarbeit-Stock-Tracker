package com.project.stocktracker.model;

import java.math.BigDecimal;

/**
 * Real-time market snapshot for one symbol.
 */
public record StockSummary(
        String symbol,
        String name,
        BigDecimal price,
        BigDecimal changePercent,
        Long volume
) {}
