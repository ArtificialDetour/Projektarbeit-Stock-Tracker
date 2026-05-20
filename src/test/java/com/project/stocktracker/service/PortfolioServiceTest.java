package com.project.stocktracker.service;

import com.project.stocktracker.dto.AssetQuoteDto;
import com.project.stocktracker.entity.Holding;
import com.project.stocktracker.entity.Transaction;
import com.project.stocktracker.entity.TransactionType;
import com.project.stocktracker.entity.User;
import com.project.stocktracker.model.DailyQuote;
import com.project.stocktracker.repository.HoldingRepository;
import com.project.stocktracker.repository.TransactionRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortfolioServiceTest {

    @Mock StockService stockService;
    @Mock TransactionRepository transactionRepository;
    @Mock HoldingRepository holdingRepository;
    @Mock EcbService ecbService;
    @Mock InflationService inflationService;

    @InjectMocks PortfolioService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@example.com");
    }

    private Holding holding(String symbol, String qty, String avgCost) {
        var h = new Holding();
        h.setUser(user);
        h.setSymbol(symbol);
        h.setAssetName("Test Asset");
        h.setQuantity(new BigDecimal(qty));
        h.setAvgCostBasis(new BigDecimal(avgCost));
        return h;
    }

    private Transaction tx(String symbol, String type, String amount) {
        var tx = new Transaction();
        tx.setUser(user);
        tx.setSymbol(symbol);
        tx.setAssetName("Test Asset");
        tx.setTransactionType(TransactionType.valueOf(type));
        tx.setTotalAmount(new BigDecimal(amount));
        return tx;
    }

    private DailyQuote quote(String date, String close) {
        return new DailyQuote(LocalDate.parse(date).atStartOfDay(),
                new BigDecimal(close), new BigDecimal(close), new BigDecimal(close), new BigDecimal(close), 0L);
    }

    @Nested
    @DisplayName("Edge Case - Empty Data")
    class EmptyData {
        @Test
        @DisplayName("getHoldings without holdings returns empty list")
        void getHoldings_noHoldings_returnsEmptyList() {
            when(holdingRepository.findByUser(user)).thenReturn(List.of());
            assertThat(service.getHoldings(user)).isEmpty();
        }
    }

    @Nested
    @DisplayName("getHoldings - Performance Calculation")
    class HoldingsPerformance {

        @Test
        @DisplayName("Performance 0% when current price = avgCostBasis")
        void performance_zeroWhenPriceEqualsAvg() {
            var h = holding("AAPL", "10", "100.00");
            var t = tx("AAPL", "BUY", "1000.00");
            when(holdingRepository.findByUser(user)).thenReturn(List.of(h));
            when(transactionRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(t));
            when(stockService.getAssetQuote("AAPL"))
                    .thenReturn(new AssetQuoteDto("AAPL", "Apple", new BigDecimal("100.00"), "USD"));
            when(stockService.getHistoricalDataForRange(any(), any(), any())).thenReturn(List.of());

            var result = service.getHoldings(user);

            assertThat(result.get(0).performancePercent()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("Positive Performance is calculated correctly (+50%)")
        void performance_positiveCalculatedCorrectly() {
            var h = holding("AAPL", "10", "100.00");
            var t = tx("AAPL", "BUY", "1000.00");
            when(holdingRepository.findByUser(user)).thenReturn(List.of(h));
            when(transactionRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(t));
            when(stockService.getAssetQuote("AAPL"))
                    .thenReturn(new AssetQuoteDto("AAPL", "Apple", new BigDecimal("150.00"), "USD"));
            when(stockService.getHistoricalDataForRange(any(), any(), any())).thenReturn(List.of());

            var result = service.getHoldings(user);

            assertThat(result.get(0).performancePercent()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("Negative Performance is calculated correctly (-25%)")
        void performance_negativeCalculatedCorrectly() {
            var h = holding("AAPL", "10", "100.00");
            var t = tx("AAPL", "BUY", "1000.00");
            when(holdingRepository.findByUser(user)).thenReturn(List.of(h));
            when(transactionRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(t));
            when(stockService.getAssetQuote("AAPL"))
                    .thenReturn(new AssetQuoteDto("AAPL", "Apple", new BigDecimal("75.00"), "USD"));
            when(stockService.getHistoricalDataForRange(any(), any(), any())).thenReturn(List.of());

            var result = service.getHoldings(user);

            assertThat(result.get(0).performancePercent()).isEqualByComparingTo("-25.00");
        }

        @Test
        @DisplayName("costBasisTotal is avgCost times quantity")
        void costBasisTotal_isAvgCostTimesQuantity() {
            var h = holding("AAPL", "10", "156.99");
            var t = tx("AAPL", "BUY", "1569.90");
            when(holdingRepository.findByUser(user)).thenReturn(List.of(h));
            when(transactionRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(t));
            when(stockService.getAssetQuote("AAPL"))
                    .thenReturn(new AssetQuoteDto("AAPL", "Apple", new BigDecimal("200.00"), "USD"));
            when(stockService.getHistoricalDataForRange(any(), any(), any())).thenReturn(List.of());

            var result = service.getHoldings(user);

            assertThat(result.get(0).costBasisTotal()).isEqualByComparingTo("1569.90");
        }

        @Test
        @DisplayName("costBasisTotal ignores already sold historical lots")
        void costBasisTotal_usesCurrentHoldingOnly() {
            var h = holding("BTC-USD", "1.01", "68848.21");
            var oldBuy = tx("BTC-USD", "BUY", "80000.00");
            var activeBuy = tx("BTC-USD", "BUY", "69536.69");
            when(holdingRepository.findByUser(user)).thenReturn(List.of(h));
            when(transactionRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(oldBuy, activeBuy));
            when(stockService.getAssetQuote("BTC-USD"))
                    .thenReturn(new AssetQuoteDto("BTC-USD", "Bitcoin USD", new BigDecimal("68853.41"), "EUR"));
            when(stockService.getHistoricalDataForRange(any(), any(), any())).thenReturn(List.of());

            var result = service.getHoldings(user);

            assertThat(result.get(0).costBasisTotal()).isEqualByComparingTo("69536.69");
        }
    }

    @Nested
    @DisplayName("Portfolio Chart")
    class PortfolioChart {

        @Test
        @DisplayName("ECB benchmark uses historical rate intervals")
        void getPortfolioChart_usesHistoricalEcbRates() {
            var tx = tx("AAPL", "BUY", "100.00");
            tx.setQuantity(BigDecimal.ONE);
            tx.setCreatedAt(LocalDateTime.of(2024, 1, 1, 0, 0));

            var rates = new TreeMap<LocalDate, BigDecimal>();
            rates.put(LocalDate.of(2024, 1, 1), new BigDecimal("4.00"));
            rates.put(LocalDate.of(2025, 1, 1), new BigDecimal("2.00"));

            when(transactionRepository.findByUserOrderByCreatedAtAsc(user)).thenReturn(List.of(tx));
            when(stockService.getHistoricalDataForRange(any(), any(), any()))
                    .thenReturn(List.of(quote("2024-01-01", "100.00"), quote("2025-01-01", "110.00")));
            when(stockService.getAssetCurrency("AAPL")).thenReturn("EUR");
            when(stockService.getFxRateToEur("EUR")).thenReturn(BigDecimal.ONE);
            when(stockService.getAssetQuote("AAPL"))
                    .thenReturn(new AssetQuoteDto("AAPL", "Apple", new BigDecimal("110.00"), "EUR"));
            when(ecbService.getCurrentEcbRate()).thenReturn(new BigDecimal("2.00"));
            when(ecbService.getHistoricalDepositRates()).thenReturn(rates);
            when(inflationService.getMonthlyRates()).thenReturn(List.of());

            var result = service.getPortfolioChart(user, "all");

            assertThat(result).hasSizeGreaterThanOrEqualTo(2);
            assertThat((BigDecimal) result.get(0).get("ecbPct")).isEqualByComparingTo("0.0000");
            assertThat((BigDecimal) result.get(1).get("ecbPct")).isBetween(
                    new BigDecimal("3.9000"),
                    new BigDecimal("4.1000")
            );
        }

        @Test
        @DisplayName("5y range requests Yahoo five-year monthly data")
        void getPortfolioChart_fiveYearRangeUsesFiveYearMonthlyData() {
            var tx = tx("AAPL", "BUY", "100.00");
            tx.setQuantity(BigDecimal.ONE);
            tx.setCreatedAt(LocalDateTime.of(2024, 1, 1, 0, 0));

            when(transactionRepository.findByUserOrderByCreatedAtAsc(user)).thenReturn(List.of(tx));
            when(stockService.getHistoricalDataForRange(any(), any(), any()))
                    .thenReturn(List.of(quote("2024-01-01", "100.00"), quote("2025-01-01", "110.00")));
            when(stockService.getAssetCurrency("AAPL")).thenReturn("EUR");
            when(stockService.getFxRateToEur("EUR")).thenReturn(BigDecimal.ONE);
            when(stockService.getAssetQuote("AAPL"))
                    .thenReturn(new AssetQuoteDto("AAPL", "Apple", new BigDecimal("110.00"), "EUR"));
            when(ecbService.getCurrentEcbRate()).thenReturn(new BigDecimal("2.00"));
            when(ecbService.getHistoricalDepositRates()).thenReturn(new TreeMap<>());
            when(inflationService.getMonthlyRates()).thenReturn(List.of());

            service.getPortfolioChart(user, "5y");

            verify(stockService).getHistoricalDataForRange(eq("AAPL"), eq("1mo"), eq("5y"));
        }

        @Test
        @DisplayName("5y chart starts at purchase date and ends with live total return")
        void getPortfolioChart_fiveYearRangeAnchorsAtPurchaseAndUsesLiveReturn() {
            var tx = tx("DAX", "BUY", "2000.00");
            tx.setQuantity(new BigDecimal("100"));
            tx.setPricePerShare(new BigDecimal("20.00"));
            tx.setCreatedAt(LocalDateTime.of(2023, 5, 6, 18, 35));

            when(transactionRepository.findByUserOrderByCreatedAtAsc(user)).thenReturn(List.of(tx));
            when(stockService.getHistoricalDataForRange("DAX", "1mo", "5y"))
                    .thenReturn(List.of(
                            quote("2021-05-01", "10.00"),
                            quote("2023-06-01", "30.80"),
                            quote("2026-05-01", "30.80")
                    ));
            when(stockService.getAssetCurrency("DAX")).thenReturn("EUR");
            when(stockService.getFxRateToEur("EUR")).thenReturn(BigDecimal.ONE);
            when(stockService.getAssetQuote("DAX"))
                    .thenReturn(new AssetQuoteDto("DAX", "Global X DAX Germany ETF", new BigDecimal("39.40"), "EUR"));
            when(ecbService.getCurrentEcbRate()).thenReturn(new BigDecimal("2.00"));
            when(ecbService.getHistoricalDepositRates()).thenReturn(new TreeMap<>());
            when(inflationService.getMonthlyRates()).thenReturn(List.of());

            var result = service.getPortfolioChart(user, "5y");

            assertThat(result.get(0).get("timestamp")).isEqualTo("2023-05-06T18:35");
            assertThat((BigDecimal) result.get(0).get("assetPct")).isEqualByComparingTo("0.0000");
            assertThat((BigDecimal) result.get(result.size() - 1).get("assetPct")).isEqualByComparingTo("97.0000");
            assertThat(result).extracting(point -> ((String) point.get("timestamp")).substring(0, 4))
                    .doesNotContain("2021", "2022");
        }

        @Test
        @DisplayName("Symbol filter calculates chart from the selected holding only")
        void getPortfolioChart_symbolFilterUsesOnlySelectedHolding() {
            var aapl = tx("AAPL", "BUY", "100.00");
            aapl.setQuantity(BigDecimal.ONE);
            aapl.setCreatedAt(LocalDateTime.of(2024, 1, 1, 0, 0));
            var msft = tx("MSFT", "BUY", "1000.00");
            msft.setQuantity(BigDecimal.TEN);
            msft.setCreatedAt(LocalDateTime.of(2024, 1, 1, 0, 0));

            when(transactionRepository.findByUserOrderByCreatedAtAsc(user)).thenReturn(List.of(aapl, msft));
            when(stockService.getHistoricalDataForRange("AAPL", "1mo", "5y"))
                    .thenReturn(List.of(quote("2024-01-01", "100.00"), quote("2025-01-01", "105.00")));
            when(stockService.getAssetCurrency("AAPL")).thenReturn("EUR");
            when(stockService.getFxRateToEur("EUR")).thenReturn(BigDecimal.ONE);
            when(stockService.getAssetQuote("AAPL"))
                    .thenReturn(new AssetQuoteDto("AAPL", "Apple", new BigDecimal("110.00"), "EUR"));
            when(ecbService.getCurrentEcbRate()).thenReturn(new BigDecimal("2.00"));
            when(ecbService.getHistoricalDepositRates()).thenReturn(new TreeMap<>());
            when(inflationService.getMonthlyRates()).thenReturn(List.of());

            var result = service.getPortfolioChart(user, "5y", "aapl");

            assertThat((BigDecimal) result.get(result.size() - 1).get("assetPct")).isEqualByComparingTo("10.0000");
            verify(stockService).getHistoricalDataForRange(eq("AAPL"), eq("1mo"), eq("5y"));
        }
    }
}
