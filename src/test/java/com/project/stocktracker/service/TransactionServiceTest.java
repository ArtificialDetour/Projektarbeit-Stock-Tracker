package com.project.stocktracker.service;

import com.project.stocktracker.dto.AssetQuoteDto;
import com.project.stocktracker.dto.BuyAssetRequestDto;
import com.project.stocktracker.dto.SellAssetRequestDto;
import com.project.stocktracker.entity.Transaction;
import com.project.stocktracker.entity.TransactionStatus;
import com.project.stocktracker.entity.TransactionType;
import com.project.stocktracker.entity.Holding;
import com.project.stocktracker.entity.User;
import com.project.stocktracker.exception.PriceChangedException;
import com.project.stocktracker.entity.UserSettings;
import java.util.List;
import com.project.stocktracker.repository.HoldingRepository;
import com.project.stocktracker.repository.NotificationRepository;
import com.project.stocktracker.repository.TransactionRepository;
import com.project.stocktracker.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionServiceTest {

    @Mock StockService            stockService;
    @Mock TransactionRepository   transactionRepository;
    @Mock HoldingRepository       holdingRepository;
    @Mock ActivityService         activityService;
    @Mock NotificationRepository  notificationRepository;
    @Mock UserSettingsRepository  userSettingsRepository;

    @InjectMocks TransactionService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@example.com");

        // Disable activity logging by default.
        when(userSettingsRepository.findByUser(any())).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // Test helpers.

    private Holding holding(String symbol, String qty, String avgCost) {
        var h = new Holding();
        h.setUser(user);
        h.setSymbol(symbol);
        h.setAssetName("Test Asset");
        h.setQuantity(new BigDecimal(qty));
        h.setAvgCostBasis(new BigDecimal(avgCost));
        return h;
    }

    private void mockQuote(String symbol, String price) {
        when(stockService.getAssetQuote(symbol))
                .thenReturn(new AssetQuoteDto(symbol, "Test Asset", new BigDecimal(price), "EUR"));
    }

    private Transaction transaction(Long id, String symbol, String type, String quantity, String price, LocalDateTime createdAt) {
        var tx = new Transaction();
        tx.setId(id);
        tx.setUser(user);
        tx.setSymbol(symbol);
        tx.setAssetName("Test Asset");
        tx.setTransactionType(TransactionType.valueOf(type));
        tx.setStatus(TransactionStatus.SETTLED);
        tx.setQuantity(new BigDecimal(quantity));
        tx.setPricePerShare(new BigDecimal(price));
        tx.setTotalAmount(new BigDecimal(quantity).multiply(new BigDecimal(price)));
        tx.setCreatedAt(createdAt);
        return tx;
    }

    // First purchase flow.
    @Nested
    @DisplayName("buyAsset - First Purchase Tests")
    class BuyFirstPurchase {

        @Test
        @DisplayName("New holding is created with correct avgCostBasis")
        void firstBuy_createsHoldingWithCorrectAvgCostBasis() {
            mockQuote("AAPL", "150.00");
            when(holdingRepository.findByUserAndSymbol(user, "AAPL")).thenReturn(Optional.empty());

            var captor = ArgumentCaptor.forClass(Holding.class);
            when(holdingRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.buyAsset(user, new BuyAssetRequestDto("AAPL", new BigDecimal("10"), new BigDecimal("150.00")));

            var saved = captor.getValue();
            assertThat(saved.getAvgCostBasis()).isEqualByComparingTo("150.00");
            assertThat(saved.getQuantity()).isEqualByComparingTo("10");
        }

        @Test
        @DisplayName("Symbol is stored in uppercase")
        void firstBuy_symbolStoredUpperCase() {
            mockQuote("aapl", "150.00");
            when(holdingRepository.findByUserAndSymbol(user, "AAPL")).thenReturn(Optional.empty());

            var captor = ArgumentCaptor.forClass(Holding.class);
            when(holdingRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.buyAsset(user, new BuyAssetRequestDto("aapl", new BigDecimal("5"), new BigDecimal("150.00")));

            assertThat(captor.getValue().getSymbol()).isEqualTo("AAPL");
        }
    }

    // Weighted average cost flow.
    @Nested
    @DisplayName("buyAsset - WAC avgCostBasis Tests")
    class BuyAvgCostBasis {

        @Test
        @DisplayName("Same price: avgCostBasis remains unchanged")
        void secondBuy_samePrice_avgUnchanged() {
            mockQuote("AAPL", "100.00");
            var existing = holding("AAPL", "10", "100.00");
            when(holdingRepository.findByUserAndSymbol(user, "AAPL")).thenReturn(Optional.of(existing));

            service.buyAsset(user, new BuyAssetRequestDto("AAPL", new BigDecimal("5"), new BigDecimal("100.00")));

            assertThat(existing.getAvgCostBasis()).isEqualByComparingTo("100.00");
            assertThat(existing.getQuantity()).isEqualByComparingTo("15");
        }

        @Test
        @DisplayName("Higher price: WAC increases correctly")
        void secondBuy_higherPrice_wacIncreasesCorrectly() {
            // WAC: (1000 + 650) / 15 = 110.
            mockQuote("AAPL", "130.00");
            var existing = holding("AAPL", "10", "100.00");
            when(holdingRepository.findByUserAndSymbol(user, "AAPL")).thenReturn(Optional.of(existing));

            service.buyAsset(user, new BuyAssetRequestDto("AAPL", new BigDecimal("5"), new BigDecimal("130.00")));

            assertThat(existing.getAvgCostBasis()).isEqualByComparingTo("110.000000");
            assertThat(existing.getQuantity()).isEqualByComparingTo("15");
        }

        @Test
        @DisplayName("Lower price: WAC decreases correctly")
        void secondBuy_lowerPrice_wacDecreasesCorrectly() {
            // WAC: (1000 + 600) / 20 = 80.
            mockQuote("AAPL", "60.00");
            var existing = holding("AAPL", "10", "100.00");
            when(holdingRepository.findByUserAndSymbol(user, "AAPL")).thenReturn(Optional.of(existing));

            service.buyAsset(user, new BuyAssetRequestDto("AAPL", new BigDecimal("10"), new BigDecimal("60.00")));

            assertThat(existing.getAvgCostBasis()).isEqualByComparingTo("80.000000");
        }

        @Test
        @DisplayName("Real-World Example: 10 @ 153.68 + 0.3 @ 267.61 = 156.99")
        void realWorldExample_wacCalculation() {
            // WAC with fractional quantity.
            mockQuote("NVDA", "267.61");
            var existing = holding("NVDA", "10", "153.68");
            when(holdingRepository.findByUserAndSymbol(user, "NVDA")).thenReturn(Optional.of(existing));

            service.buyAsset(user, new BuyAssetRequestDto("NVDA", new BigDecimal("0.3"), new BigDecimal("267.61")));

            assertThat(existing.getAvgCostBasis())
                    .usingComparator(BigDecimal::compareTo)
                    .isBetween(new BigDecimal("156.98"), new BigDecimal("157.00"));
            assertThat(existing.getQuantity()).isEqualByComparingTo("10.3");
        }
    }

    // Buy price tolerance flow.
    @Nested
    @DisplayName("buyAsset - Price Deviation Tests")
    class BuyPriceTolerance {

        @Test
        @DisplayName("Price deviation > 1% throws exception")
        void priceDeviation_aboveTolerance_throws() {
            mockQuote("AAPL", "110.00");
            when(holdingRepository.findByUserAndSymbol(any(), any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                service.buyAsset(user, new BuyAssetRequestDto("AAPL", new BigDecimal("1"), new BigDecimal("100.00")))
            ).isInstanceOf(PriceChangedException.class);
        }

        @Test
        @DisplayName("Price deviation < 1% is accepted")
        void priceDeviation_withinTolerance_succeeds() {
            mockQuote("AAPL", "100.50");
            when(holdingRepository.findByUserAndSymbol(user, "AAPL")).thenReturn(Optional.empty());
            when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatNoException().isThrownBy(() ->
                service.buyAsset(user, new BuyAssetRequestDto("AAPL", new BigDecimal("1"), new BigDecimal("100.00")))
            );
        }

        @Test
        @DisplayName("Exact price match is accepted")
        void priceDeviation_exactMatch_succeeds() {
            mockQuote("AAPL", "100.00");
            when(holdingRepository.findByUserAndSymbol(user, "AAPL")).thenReturn(Optional.empty());
            when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatNoException().isThrownBy(() ->
                service.buyAsset(user, new BuyAssetRequestDto("AAPL", new BigDecimal("1"), new BigDecimal("100.00")))
            );
        }
    }

    // Sell quantity flow.
    @Nested
    @DisplayName("sellAsset - Quantity and Holding Tests")
    class SellQuantity {

        @Test
        @DisplayName("Partial sell reduces quantity correctly")
        void partialSell_reducesQuantity() {
            mockQuote("AAPL", "150.00");
            var existing = holding("AAPL", "10", "100.00");
            when(holdingRepository.findByUserAndSymbol(user, "AAPL")).thenReturn(Optional.of(existing));

            service.sellAsset(user, new SellAssetRequestDto("AAPL", new BigDecimal("4"), new BigDecimal("150.00"), null));

            assertThat(existing.getQuantity()).isEqualByComparingTo("6");
            verify(holdingRepository).save(existing);
            verify(holdingRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Full sell deletes holding")
        void fullSell_deletesHolding() {
            mockQuote("AAPL", "150.00");
            var existing = holding("AAPL", "10", "100.00");
            when(holdingRepository.findByUserAndSymbol(user, "AAPL")).thenReturn(Optional.of(existing));

            service.sellAsset(user, new SellAssetRequestDto("AAPL", new BigDecimal("10"), new BigDecimal("150.00"), null));

            verify(holdingRepository).delete(existing);
            verify(holdingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Sell more than held throws RuntimeException")
        void sell_moreThanHeld_throwsException() {
            var existing = holding("AAPL", "5", "100.00");
            when(holdingRepository.findByUserAndSymbol(user, "AAPL")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() ->
                service.sellAsset(user, new SellAssetRequestDto("AAPL", new BigDecimal("10"), new BigDecimal("100.00"), null))
            ).isInstanceOf(RuntimeException.class)
             .hasMessageContaining("Insufficient");
        }

        @Test
        @DisplayName("No holding found throws RuntimeException")
        void sell_noHolding_throwsException() {
            when(holdingRepository.findByUserAndSymbol(user, "AAPL")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                service.sellAsset(user, new SellAssetRequestDto("AAPL", new BigDecimal("1"), new BigDecimal("100.00"), null))
            ).isInstanceOf(RuntimeException.class)
             .hasMessageContaining("No holding");
        }

        @Test
        @DisplayName("Sell with price deviation > 1% throws PriceChangedException")
        void sell_priceDeviation_throws() {
            mockQuote("AAPL", "115.00");
            var existing = holding("AAPL", "10", "100.00");
            when(holdingRepository.findByUserAndSymbol(user, "AAPL")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() ->
                service.sellAsset(user, new SellAssetRequestDto("AAPL", new BigDecimal("5"), new BigDecimal("100.00"), null))
            ).isInstanceOf(PriceChangedException.class);
        }
    }

    private UserSettings settingsWithLogging(boolean enabled) {
        var s = new UserSettings();
        s.setTransactionUpdates(enabled);
        return s;
    }

    // Zero price edge cases.
    @Nested
    @DisplayName("Edge Case - Price 0")
    class PriceZero {

        @Test
        @DisplayName("Buy with price 0 doesn't throw exception (tolerance check is skipped)")
        void buy_priceZero_noExceptionThrown() {
            mockQuote("AAPL", "0");
            when(holdingRepository.findByUserAndSymbol(user, "AAPL")).thenReturn(Optional.empty());
            when(userSettingsRepository.findByUser(any())).thenReturn(Optional.empty());

            assertThatNoException().isThrownBy(() ->
                service.buyAsset(user, new BuyAssetRequestDto("AAPL", new BigDecimal("5"), new BigDecimal("0")))
            );
        }

        @Test
        @DisplayName("Sell with price 0 doesn't throw exception")
        void sell_priceZero_noExceptionThrown() {
            mockQuote("AAPL", "0");
            var existing = holding("AAPL", "10", "100.00");
            when(holdingRepository.findByUserAndSymbol(user, "AAPL")).thenReturn(Optional.of(existing));
            when(userSettingsRepository.findByUser(any())).thenReturn(Optional.empty());

            assertThatNoException().isThrownBy(() ->
                service.sellAsset(user, new SellAssetRequestDto("AAPL", new BigDecimal("5"), new BigDecimal("0"), null))
            );
        }
    }

    // Fractional quantity edge cases.
    @Nested
    @DisplayName("Edge Case - Fractional Quantities")
    class FractionalQuantity {

        @Test
        @DisplayName("Purchase of 0.3 units is saved correctly")
        void buy_fractionalQuantity_savedCorrectly() {
            mockQuote("BTC-EUR", "50000.00");
            when(holdingRepository.findByUserAndSymbol(user, "BTC-EUR")).thenReturn(Optional.empty());
            when(userSettingsRepository.findByUser(any())).thenReturn(Optional.empty());

            var captor = ArgumentCaptor.forClass(Holding.class);
            when(holdingRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.buyAsset(user, new BuyAssetRequestDto("BTC-EUR", new BigDecimal("0.003"), new BigDecimal("50000.00")));

            assertThat(captor.getValue().getQuantity()).isEqualByComparingTo("0.003");
        }

        @Test
        @DisplayName("Partial sale with decimal places reduces quantity correctly")
        void sell_fractionalQuantity_reducesCorrectly() {
            mockQuote("BTC-EUR", "50000.00");
            var existing = holding("BTC-EUR", "0.5", "45000.00");
            when(holdingRepository.findByUserAndSymbol(user, "BTC-EUR")).thenReturn(Optional.of(existing));
            when(userSettingsRepository.findByUser(any())).thenReturn(Optional.empty());

            service.sellAsset(user, new SellAssetRequestDto("BTC-EUR", new BigDecimal("0.2"), new BigDecimal("50000.00"), null));

            assertThat(existing.getQuantity()).isEqualByComparingTo("0.3");
        }
    }

    // Activity logging flow.
    @Nested
    @DisplayName("Activity Logging")
    class ActivityLogging {

        @Test
        @DisplayName("Logging enabled: activityService is called after buy")
        void buy_loggingEnabled_activityLogged() {
            mockQuote("AAPL", "150.00");
            when(holdingRepository.findByUserAndSymbol(user, "AAPL")).thenReturn(Optional.empty());
            when(userSettingsRepository.findByUser(user))
                    .thenReturn(Optional.of(settingsWithLogging(true)));

            service.buyAsset(user, new BuyAssetRequestDto("AAPL", new BigDecimal("1"), new BigDecimal("150.00")));

            verify(activityService).logActivity(eq(user), eq("TRADE"), any(), any());
        }

        @Test
        @DisplayName("Logging disabled: activityService is not called after buy")
        void buy_loggingDisabled_noActivityLogged() {
            mockQuote("AAPL", "150.00");
            when(holdingRepository.findByUserAndSymbol(user, "AAPL")).thenReturn(Optional.empty());
            when(userSettingsRepository.findByUser(user))
                    .thenReturn(Optional.of(settingsWithLogging(false)));

            service.buyAsset(user, new BuyAssetRequestDto("AAPL", new BigDecimal("1"), new BigDecimal("150.00")));

            verify(activityService, never()).logActivity(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Logging enabled: activityService is called after sell")
        void sell_loggingEnabled_activityLogged() {
            mockQuote("AAPL", "150.00");
            var existing = holding("AAPL", "10", "100.00");
            when(holdingRepository.findByUserAndSymbol(user, "AAPL")).thenReturn(Optional.of(existing));
            when(userSettingsRepository.findByUser(user))
                    .thenReturn(Optional.of(settingsWithLogging(true)));

            service.sellAsset(user, new SellAssetRequestDto("AAPL", new BigDecimal("5"), new BigDecimal("150.00"), null));

            verify(activityService).logActivity(eq(user), eq("TRADE"), any(), any());
        }

        @Test
        @DisplayName("Logging disabled: activityService is not called after sell")
        void sell_loggingDisabled_noActivityLogged() {
            mockQuote("AAPL", "150.00");
            var existing = holding("AAPL", "10", "100.00");
            when(holdingRepository.findByUserAndSymbol(user, "AAPL")).thenReturn(Optional.of(existing));
            when(userSettingsRepository.findByUser(user))
                    .thenReturn(Optional.of(settingsWithLogging(false)));

            service.sellAsset(user, new SellAssetRequestDto("AAPL", new BigDecimal("5"), new BigDecimal("150.00"), null));

            verify(activityService, never()).logActivity(any(), any(), any(), any());
        }

        @Test
        @DisplayName("No UserSettings entry: Logging defaults to true")
        void buy_noSettings_loggingDefaultsToTrue() {
            mockQuote("AAPL", "150.00");
            when(holdingRepository.findByUserAndSymbol(user, "AAPL")).thenReturn(Optional.empty());
            when(userSettingsRepository.findByUser(user)).thenReturn(Optional.empty());

            service.buyAsset(user, new BuyAssetRequestDto("AAPL", new BigDecimal("1"), new BigDecimal("150.00")));

            verify(activityService).logActivity(eq(user), eq("TRADE"), any(), any());
        }
    }

    // Empty data edge cases.
    @Nested
    @DisplayName("Edge Case - Empty Data")
    class EmptyData {

        @Test
        @DisplayName("getTransactions without transactions returns empty list")
        void getTransactions_noTransactions_returnsEmptyList() {
            when(transactionRepository.findByUserOrderByCreatedAtAsc(user)).thenReturn(List.of());

            assertThat(service.getTransactions(user)).isEmpty();
        }

        @Test
        @DisplayName("getTransactions calculates realized profit and loss for sell rows")
        void getTransactions_calculatesRealizedGainForSells() {
            var firstBuy = transaction(1L, "AAPL", "BUY", "2", "100.00", LocalDateTime.of(2026, 5, 1, 10, 0));
            var secondBuy = transaction(2L, "AAPL", "BUY", "1", "140.00", LocalDateTime.of(2026, 5, 2, 10, 0));
            var sellProfit = transaction(3L, "AAPL", "SELL", "1.5", "120.00", LocalDateTime.of(2026, 5, 3, 10, 0));
            var sellLoss = transaction(4L, "AAPL", "SELL", "1", "90.00", LocalDateTime.of(2026, 5, 4, 10, 0));
            when(transactionRepository.findByUserOrderByCreatedAtAsc(user))
                    .thenReturn(List.of(firstBuy, secondBuy, sellProfit, sellLoss));

            var result = service.getTransactions(user);

            assertThat(result).extracting("transactionId").containsExactly(4L, 3L, 2L, 1L);
            assertThat(result.get(0).realizedGain()).isEqualByComparingTo("-30.00");
            assertThat(result.get(1).realizedGain()).isEqualByComparingTo("30.00");
            assertThat(result.get(2).realizedGain()).isNull();
            assertThat(result.get(3).realizedGain()).isNull();
        }
    }
}
