package com.project.stocktracker.controller;

import com.project.stocktracker.model.DailyQuote;
import com.project.stocktracker.model.StockSummary;
import com.project.stocktracker.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StockControllerTest {

    @Mock
    private StockService stockService;

    @InjectMocks
    private StockController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Should return real-time stock summary for given symbol")
    void getQuote_returnsStockSummary() throws Exception {
        StockSummary summary = new StockSummary("AAPL", "Apple Inc.", null, null, null);
        when(stockService.getQuote("AAPL")).thenReturn(summary);

        mockMvc.perform(get("/api/stocks/quote/AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.name").value("Apple Inc."));
    }

    @Test
    @DisplayName("Should return historical data using specific range parameter")
    void getHistory_withRange_delegatesToService() throws Exception {
        DailyQuote quote = new DailyQuote(LocalDateTime.now(), null, new BigDecimal("150.0"), null, null, null);
        
        when(stockService.getHistoricalDataForRange("AAPL", "1h", "5d")).thenReturn(List.of(quote));

        mockMvc.perform(get("/api/stocks/history/AAPL").param("range", "1w"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].close").value(150.0));
    }

    @Test
    @DisplayName("Should return historical data using default years parameter when range is omitted")
    void getHistory_withoutRange_delegatesToService() throws Exception {
        DailyQuote quote = new DailyQuote(LocalDateTime.now(), null, new BigDecimal("150.0"), null, null, null);

        when(stockService.getHistoricalData("AAPL", 1)).thenReturn(List.of(quote));

        mockMvc.perform(get("/api/stocks/history/AAPL").param("years", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].close").value(150.0));
    }

    @Test
    @DisplayName("Should return foreign exchange rate for given pair")
    void getFxRate_returnsRate() throws Exception {
        when(stockService.getFxRate("EURUSD=X")).thenReturn(new BigDecimal("1.1"));

        mockMvc.perform(get("/api/stocks/fx").param("pair", "EURUSD=X"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pair").value("EURUSD=X"))
                .andExpect(jsonPath("$.rate").value(1.1));
    }
}
