package com.project.stocktracker.exception;

import java.math.BigDecimal;

/**
 * Signals that the market price changed beyond the accepted confirmation tolerance.
 */
public class PriceChangedException extends RuntimeException {

    private final BigDecimal currentPrice;

    public PriceChangedException(BigDecimal currentPrice) {
        super("Price has changed beyond tolerance");
        this.currentPrice = currentPrice;
    }

    /**
     * Returns the latest market price that triggered the conflict.
     */
    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }
}
