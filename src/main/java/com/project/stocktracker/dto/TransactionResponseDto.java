package com.project.stocktracker.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response payload for a persisted transaction.
 */
public record TransactionResponseDto(
        Long transactionId,
        String symbol,
        String assetName,
        BigDecimal quantity,
        BigDecimal pricePerShare,
        BigDecimal totalCost,
        BigDecimal realizedGain,
        LocalDateTime timestamp,
        String status,
        String transactionType,
        Long relatedBuyId
) {}
