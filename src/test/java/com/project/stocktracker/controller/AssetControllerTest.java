package com.project.stocktracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stocktracker.dto.AssetQuoteDto;
import com.project.stocktracker.dto.AssetSearchResultDto;
import com.project.stocktracker.dto.BuyAssetRequestDto;
import com.project.stocktracker.dto.HoldingDto;
import com.project.stocktracker.dto.SellAssetRequestDto;
import com.project.stocktracker.dto.TransactionResponseDto;
import com.project.stocktracker.entity.User;
import com.project.stocktracker.exception.PriceChangedException;
import com.project.stocktracker.service.EcbService;
import com.project.stocktracker.service.PortfolioService;
import com.project.stocktracker.service.StockService;
import com.project.stocktracker.service.TransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssetControllerTest {

    @Mock StockService       stockService;
    @Mock TransactionService transactionService;
    @Mock PortfolioService   portfolioService;
    @Mock EcbService         ecbService;

    @InjectMocks AssetController controller;

    private MockMvc      mockMvc;
    private User         mockUser;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();

        mockUser = new User();
        mockUser.setEmail("test@example.com");

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        var auth = new UsernamePasswordAuthenticationToken(mockUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private TransactionResponseDto txResponse(String type) {
        return new TransactionResponseDto(
                1L, "AAPL", "Apple Inc", new BigDecimal("5"),
                new BigDecimal("150.00"), new BigDecimal("750.00"),
                null,
                LocalDateTime.now(), "SUCCESS", type, null
        );
    }

    // Asset search endpoints.
    @Nested
    @DisplayName("Search API")
    class Search {

        @Test
        @DisplayName("Blank query returns empty list")
        void blankQuery_returnsEmpty() throws Exception {
            mockMvc.perform(get("/api/assets/search").param("q", ""))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        @DisplayName("Short query returns empty list")
        void shortQuery_returnsEmpty() throws Exception {
            mockMvc.perform(get("/api/assets/search").param("q", "A"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        @DisplayName("Valid query delegates to StockService and returns results")
        void validQuery_returnsResults() throws Exception {
            when(stockService.searchAssets("AAPL"))
                    .thenReturn(List.of(new AssetSearchResultDto("AAPL", "Apple Inc", "NASDAQ")));

            mockMvc.perform(get("/api/assets/search").param("q", "AAPL"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                    .andExpect(jsonPath("$[0].name").value("Apple Inc"));
        }
    }

    // Asset quote endpoints.
    @Nested
    @DisplayName("Quote API")
    class Quote {

        @Test
        @DisplayName("Symbol is converted to uppercase")
        void symbolUppercased() throws Exception {
            when(stockService.getAssetQuote("AAPL"))
                    .thenReturn(new AssetQuoteDto("AAPL", "Apple Inc", new BigDecimal("150.00"), "EUR"));

            mockMvc.perform(get("/api/assets/quote/aapl"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.symbol").value("AAPL"));
        }

        @Test
        @DisplayName("Returns current price correctly")
        void returnsPrice() throws Exception {
            when(stockService.getAssetQuote("AAPL"))
                    .thenReturn(new AssetQuoteDto("AAPL", "Apple Inc", new BigDecimal("150.00"), "EUR"));

            mockMvc.perform(get("/api/assets/quote/AAPL"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentPrice").value(150.00));
        }
    }

    // Holding endpoints.
    @Nested
    @DisplayName("Holdings API")
    class Holdings {

        @Test
        @DisplayName("Unauthenticated access returns empty list")
        void unauthenticated_returnsEmptyList() throws Exception {
            SecurityContextHolder.clearContext();

            mockMvc.perform(get("/api/assets/holdings"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        @DisplayName("Authenticated access returns holdings")
        void authenticated_returnsHoldings() throws Exception {
            var holding = new HoldingDto(
                    "AAPL", "Apple Inc", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                    BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null, null
            );
            when(portfolioService.getHoldings(any())).thenReturn(List.of(holding));

            mockMvc.perform(get("/api/assets/holdings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].symbol").value("AAPL"));
        }

        @Test
        @DisplayName("Returns empty list when no holdings exist")
        void noHoldings_returnsEmptyList() throws Exception {
            when(portfolioService.getHoldings(any())).thenReturn(List.of());

            mockMvc.perform(get("/api/assets/holdings"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }
    }

    // Transaction list endpoints.
    @Nested
    @DisplayName("Transactions API")
    class Transactions {

        @Test
        @DisplayName("Authenticated access returns transactions")
        void authenticated_returnsTransactions() throws Exception {
            when(transactionService.getTransactions(any())).thenReturn(List.of(txResponse("BUY")));

            mockMvc.perform(get("/api/assets/transactions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].symbol").value("AAPL"));
        }

        @Test
        @DisplayName("Returns empty list when no transactions exist")
        void noTransactions_returnsEmpty() throws Exception {
            when(transactionService.getTransactions(any())).thenReturn(List.of());

            mockMvc.perform(get("/api/assets/transactions"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }
    }

    // Symbol transaction endpoints.
    @Nested
    @DisplayName("Transactions by Symbol API")
    class TransactionsBySymbol {

        @Test
        @DisplayName("Symbol is normalized to uppercase")
        void symbolUppercased() throws Exception {
            when(transactionService.getTransactionsBySymbol(any(), eq("AAPL")))
                    .thenReturn(List.of(txResponse("BUY")));

            mockMvc.perform(get("/api/assets/transactions/aapl"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].symbol").value("AAPL"));
        }
    }

    // Buy endpoints.
    @Nested
    @DisplayName("Buy API")
    class Buy {

        private String buyJson() throws Exception {
            return mapper.writeValueAsString(
                    new BuyAssetRequestDto("AAPL", new BigDecimal("5"), new BigDecimal("150.00")));
        }

        @Test
        @DisplayName("Unauthenticated access returns 401 Unauthorized")
        void unauthenticated_returns401() throws Exception {
            SecurityContextHolder.clearContext();

            mockMvc.perform(post("/api/assets/buy")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(buyJson()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Successful buy returns TransactionResponseDto")
        void success_returns200() throws Exception {
            when(transactionService.buyAsset(any(), any())).thenReturn(txResponse("BUY"));

            mockMvc.perform(post("/api/assets/buy")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(buyJson()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.symbol").value("AAPL"))
                    .andExpect(jsonPath("$.transactionType").value("BUY"));
        }

        @Test
        @DisplayName("Price change returns 409 Conflict with current price")
        void priceChanged_returns409() throws Exception {
            when(transactionService.buyAsset(any(), any()))
                    .thenThrow(new PriceChangedException(new BigDecimal("155.00")));

            mockMvc.perform(post("/api/assets/buy")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(buyJson()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.currentPrice").value(155.00));
        }
    }

    // Sell endpoints.
    @Nested
    @DisplayName("Sell API")
    class Sell {

        private String sellJson() throws Exception {
            return mapper.writeValueAsString(
                    new SellAssetRequestDto("AAPL", new BigDecimal("5"), new BigDecimal("150.00"), null));
        }

        @Test
        @DisplayName("Unauthenticated access returns 401 Unauthorized")
        void unauthenticated_returns401() throws Exception {
            SecurityContextHolder.clearContext();

            mockMvc.perform(post("/api/assets/sell")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(sellJson()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Successful sell returns TransactionResponseDto")
        void success_returns200() throws Exception {
            when(transactionService.sellAsset(any(), any())).thenReturn(txResponse("SELL"));

            mockMvc.perform(post("/api/assets/sell")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(sellJson()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transactionType").value("SELL"));
        }

        @Test
        @DisplayName("Price change returns 409 Conflict with current price")
        void priceChanged_returns409() throws Exception {
            when(transactionService.sellAsset(any(), any()))
                    .thenThrow(new PriceChangedException(new BigDecimal("145.00")));

            mockMvc.perform(post("/api/assets/sell")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(sellJson()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.currentPrice").value(145.00));
        }

        @Test
        @DisplayName("Unknown RuntimeException returns 400 Bad Request with error message")
        void runtimeException_returns400() throws Exception {
            when(transactionService.sellAsset(any(), any()))
                    .thenThrow(new RuntimeException("Insufficient shares"));

            mockMvc.perform(post("/api/assets/sell")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(sellJson()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Insufficient shares"));
        }
    }

    // Portfolio chart endpoint.
    @Nested
    @DisplayName("Portfolio Chart API")
    class PortfolioChart {

        @Test
        @DisplayName("Returns chart points and forwards selected symbol")
        void authenticated_returnsChartForSelectedSymbol() throws Exception {
            when(portfolioService.getPortfolioChart(any(), eq("5y"), eq("btc-usd")))
                    .thenReturn(List.of(Map.of(
                            "timestamp", "2026-05-07T00:00",
                            "assetPct", new BigDecimal("12.34"),
                            "ecbPct", BigDecimal.ZERO
                    )));

            mockMvc.perform(get("/api/assets/portfolio-chart")
                            .param("range", "5y")
                            .param("symbol", "btc-usd"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].timestamp").value("2026-05-07T00:00"))
                    .andExpect(jsonPath("$[0].assetPct").value(12.34));

            verify(portfolioService).getPortfolioChart(any(), eq("5y"), eq("btc-usd"));
        }
    }

    // ECB rate endpoint.
    @Nested
    @DisplayName("ECB Rate API")
    class EcbRate {

        @Test
        @DisplayName("Returns current ECB rate")
        void returnsRate() throws Exception {
            when(ecbService.getCurrentEcbRate()).thenReturn(new BigDecimal("1.0823"));

            mockMvc.perform(get("/api/assets/ecb-rate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rate").value(1.0823));
        }
    }
}
