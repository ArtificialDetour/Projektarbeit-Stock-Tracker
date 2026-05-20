package com.project.stocktracker.dto;

import java.math.BigDecimal;

/**
 * Request payload for creating a sell transaction.
 */
public record SellAssetRequestDto(
    String symbol, 
    BigDecimal quantity, 
    BigDecimal expectedPrice, 
    Long relatedBuyId
    ) {}
