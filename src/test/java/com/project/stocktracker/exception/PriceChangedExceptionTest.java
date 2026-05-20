package com.project.stocktracker.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PriceChangedExceptionTest {

    @Test
    @DisplayName("Should correctly initialize message and current price")
    void constructor_initializesCorrectly() {
        BigDecimal price = new BigDecimal("155.50");
        PriceChangedException exception = new PriceChangedException(price);

        assertThat(exception.getMessage()).isEqualTo("Price has changed beyond tolerance");
        assertThat(exception.getCurrentPrice()).isEqualTo(price);
    }
}
