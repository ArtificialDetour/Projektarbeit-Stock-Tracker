package com.project.stocktracker.service;

import com.project.stocktracker.dto.AssetQuoteDto;
import com.project.stocktracker.dto.AssetSearchResultDto;
import com.project.stocktracker.model.DailyQuote;
import com.project.stocktracker.model.StockSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @InjectMocks
    private StockService stockService;

    private HttpClient httpClientMock;

    @BeforeEach
    void setUp() {
        httpClientMock = mock(HttpClient.class);
        ReflectionTestUtils.setField(stockService, "http", httpClientMock);
    }

    @Test
    @DisplayName("Should return fallback summary when quote fetch fails")
    void getQuote_exception_returnsFallback() throws Exception {
        when(httpClientMock.send(any(), any())).thenThrow(new RuntimeException("API down"));

        StockSummary result = stockService.getQuote("AAPL");

        assertThat(result.symbol()).isEqualTo("AAPL");
        assertThat(result.price()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should return empty list for historical data on failure")
    void getHistoricalData_exception_returnsEmptyList() throws Exception {
        when(httpClientMock.send(any(), any())).thenThrow(new RuntimeException("API down"));

        List<DailyQuote> data = stockService.getHistoricalData("AAPL", 5);

        assertThat(data).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for search on failure")
    void searchAssets_exception_returnsEmptyList() throws Exception {
        when(httpClientMock.send(any(), any())).thenThrow(new RuntimeException("API down"));

        List<AssetSearchResultDto> results = stockService.searchAssets("AAPL");

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Multiple quotes should handle failures gracefully")
    void getMultipleQuotes_returnsFallbacks() throws Exception {
        when(httpClientMock.send(any(), any())).thenThrow(new RuntimeException("API down"));

        Map<String, StockSummary> quotes = stockService.getMultipleQuotes(List.of("AAPL", "MSFT"));

        assertThat(quotes).hasSize(2);
        assertThat(quotes.get("AAPL").price()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(quotes.get("MSFT").price()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
