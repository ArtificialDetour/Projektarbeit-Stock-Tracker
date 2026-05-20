package com.project.stocktracker.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request payload for creating a buy transaction.
 */
public record BuyAssetRequestDto(
        String symbol,
        BigDecimal quantity,
        BigDecimal expectedPrice,
        BigDecimal customPrice,
        LocalDateTime purchasedAt) {

    public BuyAssetRequestDto(String symbol, BigDecimal quantity, BigDecimal expectedPrice) {
        this(symbol, quantity, expectedPrice, null, null);
    }
}
