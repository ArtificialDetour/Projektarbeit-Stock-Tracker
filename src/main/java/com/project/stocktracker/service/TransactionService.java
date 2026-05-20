package com.project.stocktracker.service;

import com.project.stocktracker.dto.BuyAssetRequestDto;
import com.project.stocktracker.dto.SellAssetRequestDto;
import com.project.stocktracker.dto.TransactionResponseDto;
import com.project.stocktracker.entity.*;
import com.project.stocktracker.exception.PriceChangedException;
import com.project.stocktracker.repository.HoldingRepository;
import com.project.stocktracker.repository.NotificationRepository;
import com.project.stocktracker.repository.TransactionRepository;
import com.project.stocktracker.repository.UserSettingsRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates buy and sell transactions and keeps holdings synchronized.
 */
@Service
public class TransactionService {

    // Reject market-price drift above 1% unless the user supplied a custom execution price.
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    private final StockService stockService;
    private final TransactionRepository transactionRepository;
    private final HoldingRepository holdingRepository;
    private final ActivityService activityService;
    private final NotificationRepository notificationRepository;
    private final UserSettingsRepository userSettingsRepository;

    /**
     * Creates a transaction service with market data and persistence dependencies.
     */
    public TransactionService(StockService stockService, TransactionRepository transactionRepository, HoldingRepository holdingRepository, ActivityService activityService, NotificationRepository notificationRepository, UserSettingsRepository userSettingsRepository) {
        this.stockService = stockService;
        this.transactionRepository = transactionRepository;
        this.holdingRepository = holdingRepository;
        this.activityService = activityService;
        this.notificationRepository = notificationRepository;
        this.userSettingsRepository = userSettingsRepository;
    }

    /**
     * Executes a buy order and updates or creates the related holding.
     */
    @Transactional
    public TransactionResponseDto buyAsset(User user, BuyAssetRequestDto req) {
        var quote = stockService.getAssetQuote(req.symbol());
        var currentPrice = getBuyPrice(req, quote.currentPrice());
        var purchasedAt = req.purchasedAt() != null ? req.purchasedAt() : LocalDateTime.now();

        // Live-price buys require confirmation when the quote moved too far from the UI preview.
        validatePriceDrift(currentPrice, req.expectedPrice(), req.customPrice() != null);

        var totalAmount = currentPrice.multiply(req.quantity()).setScale(2, RoundingMode.HALF_UP);

        var tx = new Transaction();
        tx.setUser(user);
        tx.setSymbol(req.symbol().toUpperCase());
        tx.setAssetName(quote.name());
        tx.setQuantity(req.quantity());
        tx.setPricePerShare(currentPrice);
        tx.setTotalAmount(totalAmount);
        tx.setTransactionType(TransactionType.BUY);
        tx.setStatus(TransactionStatus.SETTLED);
        tx.setCreatedAt(purchasedAt);
        transactionRepository.save(tx);

        holdingRepository.findByUserAndSymbol(user, req.symbol().toUpperCase())
                .ifPresentOrElse(
                        h -> updateHolding(h, req.quantity(), currentPrice, purchasedAt),
                        () -> createHolding(user, req.symbol().toUpperCase(), quote.name(), req.quantity(),
                                currentPrice, purchasedAt));

        logTransactionActivity(user, "Buy", req.quantity(), quote.name(), currentPrice);

        return toDto(tx);
    }

    /**
     * Executes a sell order and reduces or removes the related holding.
     */
    @Transactional
    public TransactionResponseDto sellAsset(User user, SellAssetRequestDto req) {
        var holding = holdingRepository.findByUserAndSymbol(user, req.symbol().toUpperCase())
                .orElseThrow(() -> new RuntimeException("No holding found for " + req.symbol()));

        if (holding.getQuantity().compareTo(req.quantity()) < 0) {
            throw new RuntimeException("Insufficient shares: you hold " + holding.getQuantity().toPlainString());
        }

        var quote = stockService.getAssetQuote(req.symbol());
        var currentPrice = quote.currentPrice();

        // Sells always use the current quote, so stale UI prices must be confirmed.
        validatePriceDrift(currentPrice, req.expectedPrice(), false);

        var totalAmount = currentPrice.multiply(req.quantity()).setScale(2, RoundingMode.HALF_UP);

        var tx = new Transaction();
        tx.setUser(user);
        tx.setSymbol(req.symbol().toUpperCase());
        tx.setAssetName(quote.name());
        tx.setQuantity(req.quantity());
        tx.setPricePerShare(currentPrice);
        tx.setTotalAmount(totalAmount);
        tx.setTransactionType(TransactionType.SELL);
        tx.setStatus(TransactionStatus.SETTLED);
        tx.setRelatedBuyId(req.relatedBuyId());
        transactionRepository.save(tx);

        var newQty = holding.getQuantity().subtract(req.quantity());
        if (newQty.compareTo(BigDecimal.ZERO) == 0) {
            holdingRepository.delete(holding);
        } else {
            holding.setQuantity(newQty);
            holdingRepository.save(holding);
        }

        logTransactionActivity(user, "Sell", req.quantity(), quote.name(), currentPrice);

        return toDto(tx);
    }

    /**
     * Returns all transactions for a user, newest first.
     */
    public List<TransactionResponseDto> getTransactions(User user) {
        var transactions = transactionRepository.findByUserOrderByCreatedAtAsc(user);
        var realizedGains = calculateRealizedGains(transactions);

        return transactions.stream()
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                .map(tx -> toDto(tx, realizedGains.get(tx.getId())))
                .toList();
    }

    /**
     * Returns all transactions for a user's symbol, oldest first.
     */
    public List<TransactionResponseDto> getTransactionsBySymbol(User user, String symbol) {
        var transactions = transactionRepository.findByUserAndSymbolOrderByCreatedAtAsc(user, symbol);
        var realizedGains = calculateRealizedGains(transactions);

        return transactions.stream()
                .map(tx -> toDto(tx, realizedGains.get(tx.getId())))
                .toList();
    }

    private TransactionResponseDto toDto(Transaction tx) {
        return toDto(tx, null);
    }

    private TransactionResponseDto toDto(Transaction tx, BigDecimal realizedGain) {
        return new TransactionResponseDto(
                tx.getId(), tx.getSymbol(), tx.getAssetName(),
                tx.getQuantity(), tx.getPricePerShare(), tx.getTotalAmount(),
                realizedGain,
                tx.getCreatedAt(), tx.getStatus().name(), tx.getTransactionType().name(),
                tx.getRelatedBuyId());
    }

    private Map<Long, BigDecimal> calculateRealizedGains(List<Transaction> transactions) {
        Map<String, List<OpenLot>> lotsBySymbol = new HashMap<>();
        Map<Long, BigDecimal> realizedGains = new HashMap<>();

        for (var tx : transactions) {
            var symbolLots = lotsBySymbol.computeIfAbsent(tx.getSymbol(), ignored -> new ArrayList<>());

            if (tx.getTransactionType() == TransactionType.BUY) {
                symbolLots.add(new OpenLot(tx.getId(), tx.getQuantity(), tx.getPricePerShare()));
                continue;
            }

            if (tx.getTransactionType() == TransactionType.SELL) {
                var realizedGain = tx.getRelatedBuyId() != null
                        ? realizeSpecificLot(symbolLots, tx)
                        : realizeFifo(symbolLots, tx);
                if (realizedGain != null) {
                    realizedGains.put(tx.getId(), realizedGain.setScale(2, RoundingMode.HALF_UP));
                }
            }
        }

        return realizedGains;
    }

    private BigDecimal realizeSpecificLot(List<OpenLot> lots, Transaction sell) {
        return lots.stream()
                .filter(lot -> lot.buyId().equals(sell.getRelatedBuyId()))
                .findFirst()
                .filter(lot -> lot.remainingQuantity().compareTo(sell.getQuantity()) >= 0)
                .map(lot -> {
                    lot.reduce(sell.getQuantity());
                    return sell.getPricePerShare()
                            .subtract(lot.pricePerShare())
                            .multiply(sell.getQuantity());
                })
                .orElse(null);
    }

    private BigDecimal realizeFifo(List<OpenLot> lots, Transaction sell) {
        var remainingToSell = sell.getQuantity();
        var gain = BigDecimal.ZERO;

        for (var lot : lots) {
            if (remainingToSell.compareTo(BigDecimal.ZERO) <= 0) break;
            if (lot.remainingQuantity().compareTo(BigDecimal.ZERO) <= 0) continue;

            var soldFromLot = lot.remainingQuantity().min(remainingToSell);
            gain = gain.add(sell.getPricePerShare()
                    .subtract(lot.pricePerShare())
                    .multiply(soldFromLot));
            lot.reduce(soldFromLot);
            remainingToSell = remainingToSell.subtract(soldFromLot);
        }

        return remainingToSell.compareTo(BigDecimal.ZERO) == 0 ? gain : null;
    }

    private static final class OpenLot {
        private final Long buyId;
        private BigDecimal remainingQuantity;
        private final BigDecimal pricePerShare;

        private OpenLot(Long buyId, BigDecimal remainingQuantity, BigDecimal pricePerShare) {
            this.buyId = buyId;
            this.remainingQuantity = remainingQuantity;
            this.pricePerShare = pricePerShare;
        }

        private Long buyId() {
            return buyId;
        }

        private BigDecimal remainingQuantity() {
            return remainingQuantity;
        }

        private BigDecimal pricePerShare() {
            return pricePerShare;
        }

        private void reduce(BigDecimal quantity) {
            remainingQuantity = remainingQuantity.subtract(quantity);
        }
    }

    private void updateHolding(Holding h, BigDecimal addQty, BigDecimal price, LocalDateTime purchasedAt) {
        var oldTotal = h.getAvgCostBasis().multiply(h.getQuantity());
        var newTotal = price.multiply(addQty);
        var newQty = h.getQuantity().add(addQty);
        h.setQuantity(newQty);
        // Weighted average cost basis keeps follow-up purchases from overwriting earlier entry prices.
        h.setAvgCostBasis(oldTotal.add(newTotal).divide(newQty, 6, RoundingMode.HALF_UP));
        if (purchasedAt != null && (h.getFirstPurchaseDate() == null || purchasedAt.isBefore(h.getFirstPurchaseDate()))) {
            h.setFirstPurchaseDate(purchasedAt);
        }
        holdingRepository.save(h);
    }

    private BigDecimal getBuyPrice(BuyAssetRequestDto req, BigDecimal quotedPrice) {
        if (req.customPrice() != null && req.customPrice().compareTo(BigDecimal.ZERO) > 0) {
            return req.customPrice();
        }
        return quotedPrice;
    }

    private void createHolding(User user, String symbol, String name, BigDecimal qty, BigDecimal price, LocalDateTime purchasedAt) {
        var h = new Holding();
        h.setUser(user);
        h.setSymbol(symbol);
        h.setAssetName(name);
        h.setQuantity(qty);
        h.setAvgCostBasis(price);
        h.setFirstPurchaseDate(purchasedAt);
        holdingRepository.save(h);
    }

    private void validatePriceDrift(BigDecimal currentPrice, BigDecimal expectedPrice, boolean isCustomPrice) {
        if (!isCustomPrice && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
            var diff = currentPrice.subtract(expectedPrice).abs()
                    .divide(expectedPrice, 6, RoundingMode.HALF_UP);
            if (diff.compareTo(TOLERANCE) > 0) {
                throw new PriceChangedException(currentPrice);
            }
        }
    }

    private void logTransactionActivity(User user, String action, BigDecimal quantity, String assetName, BigDecimal price) {
        boolean shouldLog = userSettingsRepository.findByUser(user)
                .map(UserSettings::isTransactionUpdates)
                .orElse(true);

        if (shouldLog) {
            String title = action.equals("Buy") ? "Buy order executed" : "Sell order executed";
            String pastTense = action.equals("Buy") ? "Purchased" : "Sold";
            activityService.logActivity(user, "TRADE", title,
                    String.format("%s %s shares of %s at %s€",
                            pastTense, quantity.toPlainString(), assetName, price.toPlainString()));
        }
    }
}
