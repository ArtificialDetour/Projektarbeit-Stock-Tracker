package com.project.stocktracker.dto;

import java.math.BigDecimal;

/**
 * Quote payload with a normalized current price.
 */
public record AssetQuoteDto(
    String symbol, 
    String name, 
    BigDecimal currentPrice, 
    String currency
    ) {}
