package com.project.stocktracker.service;

import com.project.stocktracker.dto.HoldingDto;
import com.project.stocktracker.entity.Transaction;
import com.project.stocktracker.entity.TransactionType;
import com.project.stocktracker.entity.User;
import com.project.stocktracker.model.DailyQuote;
import com.project.stocktracker.repository.HoldingRepository;
import com.project.stocktracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Builds current holdings and portfolio benchmark chart data.
 */
@Service
public class PortfolioService {

    private static final ExecutorService IO_POOL = Executors.newCachedThreadPool();

    private final StockService stockService;
    private final TransactionRepository transactionRepository;
    private final HoldingRepository holdingRepository;
    private final EcbService ecbService;
    private final InflationService inflationService;

    /**
     * Creates a portfolio service with market, transaction, and benchmark dependencies.
     */
    public PortfolioService(StockService stockService, TransactionRepository transactionRepository, HoldingRepository holdingRepository, EcbService ecbService, InflationService inflationService) {
        this.stockService = stockService;
        this.transactionRepository = transactionRepository;
        this.holdingRepository = holdingRepository;
        this.ecbService = ecbService;
        this.inflationService = inflationService;
    }

    /**
     * Returns current holdings enriched with live price and performance values.
     */
    public List<HoldingDto> getHoldings(User user) {
        var holdings = holdingRepository.findByUser(user);

        // Load quote and weekly change in parallel per holding
        var futures = holdings.stream().map(h -> {
            var quoteFuture  = CompletableFuture.supplyAsync(() -> stockService.getAssetQuote(h.getSymbol()),  IO_POOL);
            var weeklyFuture = CompletableFuture.supplyAsync(() -> computeWeeklyChange(h.getSymbol()), IO_POOL);
            return quoteFuture.thenCombine(weeklyFuture, (quote, weeklyPct) -> {
                var currentPrice = quote.currentPrice();
                var qty          = h.getQuantity();
                var currentValue = currentPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);
                var costTotal = h.getAvgCostBasis().multiply(qty).setScale(2, RoundingMode.HALF_UP);

                var perf = BigDecimal.ZERO;
                if (costTotal.compareTo(BigDecimal.ZERO) != 0) {
                    perf = currentValue.subtract(costTotal)
                            .divide(costTotal.abs(), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                            .setScale(2, RoundingMode.HALF_UP);
                } else if (currentValue.compareTo(BigDecimal.ZERO) > 0) {
                    perf = new BigDecimal("100.00");
                }

                var firstDate = h.getFirstPurchaseDate() != null ? h.getFirstPurchaseDate().toLocalDate() : null;
                return new HoldingDto(h.getSymbol(), h.getAssetName(), qty, h.getAvgCostBasis(),
                        costTotal, currentPrice, currentValue, perf, weeklyPct, firstDate);
            });
        }).toList();

        return futures.stream().map(CompletableFuture::join).toList();
    }

    private BigDecimal computeWeeklyChange(String symbol) {
        try {
            var hist = stockService.getHistoricalDataForRange(symbol, "1d", "5d");
            if (hist.size() < 2)
                return BigDecimal.ZERO;
            var first = hist.get(0).close();
            var last = hist.get(hist.size() - 1).close();
            if (first.compareTo(BigDecimal.ZERO) == 0)
                return BigDecimal.ZERO;
            return last.subtract(first)
                    .divide(first, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Returns percentage-based chart data: { timestamp, assetPct, ecbPct, inflationPct }.
     * - assetPct = (marketValue - costBasis) / costBasis * 100
     * - ecbPct = (totalHypValue - totalInvested) / totalInvested * 100
     *   where hypValue = sum of each BUY amount compounded at the ECB rate
     * - inflationPct = cumulative inflation % using Eurostat HICP monthly rates
     */
    public List<Map<String, Object>> getPortfolioChart(User user, String range) {
        return getPortfolioChart(user, range, null);
    }

    public List<Map<String, Object>> getPortfolioChart(User user, String range, String symbol) {
        var transactions = transactionRepository.findByUserOrderByCreatedAtAsc(user);
        if (symbol != null && !symbol.isBlank()) {
            var normalizedSymbol = symbol.toUpperCase();
            transactions = transactions.stream()
                    .filter(tx -> tx.getSymbol().equals(normalizedSymbol))
                    .toList();
        }
        if (transactions.isEmpty())
            return List.of();

        // intraday=true: Price search and time window filter per LocalDateTime (minute-exact)
        // intraday=false: Price search per LocalDate (day-exact, sufficient for daily/weekly/monthly)
        record RangeCfg(String yahooRange, String interval, boolean intraday) {
        }

        RangeCfg cfg;
        var firstTxTimestamp = transactions.get(0).getCreatedAt();
        var firstTxDate = firstTxTimestamp.toLocalDate();
        if (range.equalsIgnoreCase("all")) {
            long daysSinceFirst = java.time.temporal.ChronoUnit.DAYS.between(firstTxDate, LocalDate.now());
            if (daysSinceFirst <= 7)
                cfg = new RangeCfg("5d",  "5m",  true);
            else if (daysSinceFirst <= 30)
                cfg = new RangeCfg("1mo", "1d",  false);
            else if (daysSinceFirst <= 90)
                cfg = new RangeCfg("3mo", "1d",  false);
            else if (daysSinceFirst <= 365)
                cfg = new RangeCfg("1y",  "1wk", false);
            else
                cfg = new RangeCfg("5y",  "1mo", false);
        } else {
            cfg = switch (range.toLowerCase()) {
                // 5d/5m instead of 1d/5m: Yahoo often returns empty for range=1d outside trading hours
                case "1d" -> new RangeCfg("5d",  "5m", true);
                case "1w" -> new RangeCfg("5d",  "1h", true);
                case "1m" -> new RangeCfg("1mo", "1d",  false);
                case "3m" -> new RangeCfg("3mo", "1d",  false);
                case "1y" -> new RangeCfg("1y",  "1wk", false);
                case "5y" -> new RangeCfg("5y",  "1mo", false);
                default   -> new RangeCfg("5y",  "1mo", false);
            };
        }

        var allSymbols = transactions.stream()
                .map(Transaction::getSymbol)
                .distinct()
                .toList();

        // Load historical prices for all symbols in parallel
        var histFutures = allSymbols.stream()
                .map(sym -> CompletableFuture.supplyAsync(
                        () -> Map.entry(sym, stockService.getHistoricalDataForRange(sym, cfg.interval(), cfg.yahooRange())),
                        IO_POOL))
                .toList();
        var histMap = new LinkedHashMap<String, List<DailyQuote>>();
        histFutures.stream()
                .map(CompletableFuture::join)
                .filter(e -> !e.getValue().isEmpty())
                .forEach(e -> histMap.put(e.getKey(), e.getValue()));
        if (histMap.isEmpty())
            return List.of();

        // Load FX rate per symbol (parallel) so all prices are in EUR
        var fxFutures = histMap.keySet().stream()
                .map(sym -> CompletableFuture.supplyAsync(
                        () -> Map.entry(sym, stockService.getFxRateToEur(stockService.getAssetCurrency(sym))),
                        IO_POOL))
                .toList();
        var fxRates = new LinkedHashMap<String, BigDecimal>();
        fxFutures.stream()
                .map(CompletableFuture::join)
                .forEach(e -> fxRates.put(e.getKey(), e.getValue()));

        var refTs = histMap.values().stream()
                .max(java.util.Comparator.comparingInt(List::size))
                .orElse(List.of())
                .stream().map(DailyQuote::date).toList();

        var currentEcbRate = ecbService.getCurrentEcbRate();
        var historicalEcbRates = ecbService.getHistoricalDepositRates();

        // Inflation: month -> annual rate from InflationService (Eurostat HICP)
        var inflRateMap = new java.util.TreeMap<String, Double>();
        try {
            inflationService.getMonthlyRates().forEach(entry ->
                    inflRateMap.put((String) entry.get("month"), ((Number) entry.get("rate")).doubleValue()));
        } catch (Exception ignored) {}

        List<LocalDateTime> rangeTs;
        if (range.equalsIgnoreCase("all") || range.equalsIgnoreCase("5y")) {
            rangeTs = new ArrayList<>(refTs);
        } else if (range.equalsIgnoreCase("1d")) {
            // Last 24 hours; fallback to last 40 candles on weekends/holidays
            var cutoff = LocalDateTime.now().minusHours(24);
            rangeTs = refTs.stream()
                    .filter(ts -> !ts.isBefore(cutoff))
                    .collect(Collectors.toCollection(ArrayList::new));
            if (rangeTs.size() < 2)
                rangeTs = new ArrayList<>(refTs.subList(Math.max(0, refTs.size() - 40), refTs.size()));
        } else {
            rangeTs = new ArrayList<>(refTs);
        }

        var filteredTs = applyPurchaseStart(rangeTs, firstTxTimestamp, cfg.intraday());
        if (filteredTs.size() < 2 && refTs.size() >= 2)
            filteredTs = applyPurchaseStart(new ArrayList<>(refTs.subList(Math.max(0, refTs.size() - 10), refTs.size())),
                    firstTxTimestamp, cfg.intraday());
        if (filteredTs.isEmpty())
            return List.of();

        // Set last data point to today if the last candle is before today
        // (occurs with monthly/weekly intervals, e.g. last point = April 1st instead of April 30th)
        var today = LocalDate.now();
        if (filteredTs.get(filteredTs.size() - 1).toLocalDate().isBefore(today))
            filteredTs.add(LocalDateTime.now());

        var intraday = cfg.intraday();
        var livePrices = loadLivePricesForTodayPoint(filteredTs, histMap, fxRates);
        var result = new ArrayList<Map<String, Object>>();
        double cumulativeInflation = 0.0;
        LocalDateTime prevTs = null;
        BigDecimal baseRawAssetPct = null;
        BigDecimal lastRawAssetPct = BigDecimal.ZERO;

        for (int ti = 0; ti < filteredTs.size(); ti++) {
            var ts     = filteredTs.get(ti);
            var tsDate = ts.toLocalDate();
            var isStartPoint = ts.equals(firstTxTimestamp);
            var isTodayPoint = ti == filteredTs.size() - 1 && ts.toLocalDate().equals(today);
            var totalMarketValue = BigDecimal.ZERO;
            var totalInvested    = BigDecimal.ZERO;

            for (var sym : histMap.keySet()) {
                var qtyHeld     = BigDecimal.ZERO;
                var symInvested = BigDecimal.ZERO;

                for (var tx : transactions) {
                    if (!tx.getSymbol().equals(sym)) continue;
                    if (tx.getCreatedAt().isAfter(ts)) continue;

                    if (tx.getTransactionType() == TransactionType.BUY) {
                        qtyHeld     = qtyHeld.add(tx.getQuantity());
                        symInvested = symInvested.add(tx.getTotalAmount());
                    } else if (tx.getTransactionType() == TransactionType.SELL) {
                        qtyHeld     = qtyHeld.subtract(tx.getQuantity()).max(BigDecimal.ZERO);
                        symInvested = symInvested.subtract(tx.getTotalAmount());
                    }
                }

                if (qtyHeld.compareTo(BigDecimal.ZERO) > 0) {
                    var symHist = histMap.get(sym);
                    BigDecimal price = resolveChartPrice(sym, symHist, ts, tsDate, intraday, isTodayPoint, livePrices);

                    // Historical prices come from Yahoo in the asset currency; live asset quotes are already in EUR.
                    if (!isTodayPoint) {
                        var fxRate = fxRates.getOrDefault(sym, BigDecimal.ONE);
                        price = price.multiply(fxRate);
                    }

                    totalMarketValue = totalMarketValue.add(price.multiply(qtyHeld));
                    totalInvested    = totalInvested.add(symInvested.max(BigDecimal.ZERO));
                }
            }

            if (isStartPoint) {
                totalMarketValue = totalInvested;
            }

            BigDecimal rawAssetPct = BigDecimal.ZERO;
            if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
                rawAssetPct = totalMarketValue
                        .subtract(totalInvested)
                        .divide(totalInvested, 6, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(4, RoundingMode.HALF_UP);
                lastRawAssetPct = rawAssetPct;
            } else {
                rawAssetPct = lastRawAssetPct;
            }

            if (ti == 0) {
                baseRawAssetPct = rawAssetPct;
            }

            BigDecimal assetPct;
            if (firstTxTimestamp.isBefore(filteredTs.get(0))) {
                assetPct = rawAssetPct.subtract(baseRawAssetPct);
            } else {
                assetPct = rawAssetPct;
            }

            var ecbPct = calculateEcbBenchmarkPct(filteredTs.get(0), ts, historicalEcbRates, currentEcbRate);

            // Actual monthly inflation rate (mountain curve instead of smooth compound interest curve)
            double annualRate = 2.0;
            var curMonth = String.format("%d-%02d", ts.getYear(), ts.getMonthValue());
            if (inflRateMap.containsKey(curMonth)) {
                annualRate = inflRateMap.get(curMonth);
            } else if (ti > 0) {
                var prev = filteredTs.get(ti - 1);
                var prevMonth = String.format("%d-%02d", prev.getYear(), prev.getMonthValue());
                annualRate = inflRateMap.getOrDefault(prevMonth, 2.0);
            }
            var inflationPct = BigDecimal.valueOf(annualRate).setScale(4, RoundingMode.HALF_UP);

            // Accumulate inflation proportional to elapsed time since previous data point
            if (prevTs != null) {
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(prevTs.toLocalDate(), ts.toLocalDate());
                double yearFraction = daysBetween / 365.25;
                cumulativeInflation += annualRate * yearFraction;
            }
            var realReturnPct = assetPct.subtract(BigDecimal.valueOf(cumulativeInflation))
                    .setScale(4, RoundingMode.HALF_UP);
            var cumulativeInflationPct = BigDecimal.valueOf(cumulativeInflation)
                    .setScale(4, RoundingMode.HALF_UP);

            var point = new LinkedHashMap<String, Object>();
            point.put("timestamp",              ts.toString());
            point.put("assetPct",               assetPct);
            point.put("ecbPct",                 ecbPct);
            point.put("inflationPct",           inflationPct);
            point.put("realReturnPct",          realReturnPct);
            point.put("cumulativeInflationPct", cumulativeInflationPct);
            result.add(point);
            prevTs = ts;
        }

        return result;
    }

    private List<LocalDateTime> applyPurchaseStart(
            List<LocalDateTime> rangeTimestamps,
            LocalDateTime firstTxTimestamp,
            boolean intraday) {
        if (rangeTimestamps.isEmpty()) {
            return new ArrayList<>(List.of(firstTxTimestamp));
        }

        var firstRangeTs = rangeTimestamps.get(0);
        if (!firstTxTimestamp.isAfter(firstRangeTs)) {
            return rangeTimestamps;
        }

        var filtered = rangeTimestamps.stream()
                .filter(ts -> intraday
                        ? ts.isAfter(firstTxTimestamp)
                        : ts.toLocalDate().isAfter(firstTxTimestamp.toLocalDate()))
                .collect(Collectors.toCollection(ArrayList::new));
        filtered.add(0, firstTxTimestamp);
        return filtered;
    }

    private Map<String, BigDecimal> loadLivePricesForTodayPoint(
            List<LocalDateTime> timestamps,
            Map<String, List<DailyQuote>> historyBySymbol,
            Map<String, BigDecimal> fxRates) {
        if (timestamps.isEmpty() || !timestamps.get(timestamps.size() - 1).toLocalDate().equals(LocalDate.now())) {
            return Map.of();
        }

        var futures = historyBySymbol.keySet().stream()
                .map(sym -> {
                    var fallback = latestHistoricalPriceInEur(sym, historyBySymbol.get(sym), fxRates);
                    return CompletableFuture.supplyAsync(() -> stockService.getAssetQuote(sym).currentPrice(), IO_POOL)
                            .completeOnTimeout(fallback, 2, TimeUnit.SECONDS)
                            .exceptionally(ignored -> fallback)
                            .thenApply(price -> Map.entry(sym, price));
                })
                .toList();

        var livePrices = new LinkedHashMap<String, BigDecimal>();
        futures.stream().map(CompletableFuture::join).forEach(entry -> livePrices.put(entry.getKey(), entry.getValue()));
        return livePrices;
    }

    private BigDecimal latestHistoricalPriceInEur(
            String symbol,
            List<DailyQuote> history,
            Map<String, BigDecimal> fxRates) {
        if (history == null || history.isEmpty()) {
            return BigDecimal.ZERO;
        }

        var price = history.get(history.size() - 1).close();
        return price.multiply(fxRates.getOrDefault(symbol, BigDecimal.ONE));
    }

    private BigDecimal resolveChartPrice(
            String symbol,
            List<DailyQuote> history,
            LocalDateTime ts,
            LocalDate tsDate,
            boolean intraday,
            boolean isTodayPoint,
            Map<String, BigDecimal> livePrices) {
        if (isTodayPoint && livePrices.containsKey(symbol)) {
            return livePrices.get(symbol);
        }

        BigDecimal price = BigDecimal.ZERO;
        for (int i = history.size() - 1; i >= 0; i--) {
            // Intraday: compare minute-exact timestamp; Daily: compare date only.
            boolean match = intraday
                    ? !history.get(i).date().isAfter(ts)
                    : !history.get(i).date().toLocalDate().isAfter(tsDate);
            if (match) {
                price = history.get(i).close();
                break;
            }
        }
        if (price.compareTo(BigDecimal.ZERO) == 0 && !history.isEmpty()) {
            price = history.get(0).close();
        }

        return price;
    }

    private BigDecimal calculateEcbBenchmarkPct(
            LocalDateTime start,
            LocalDateTime end,
            NavigableMap<LocalDate, BigDecimal> historicalRates,
            BigDecimal fallbackAnnualRate) {
        if (!end.isAfter(start)) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        LocalDateTime cursor = start;
        double factor = 1.0;

        while (cursor.isBefore(end)) {
            BigDecimal annualRate = resolveEcbRate(cursor.toLocalDate(), historicalRates, fallbackAnnualRate);
            var nextRateEntry = historicalRates == null ? null : historicalRates.higherEntry(cursor.toLocalDate());
            LocalDateTime nextBoundary = nextRateEntry == null ? end : nextRateEntry.getKey().atStartOfDay();
            LocalDateTime segmentEnd = nextBoundary.isAfter(cursor) && nextBoundary.isBefore(end) ? nextBoundary : end;

            long seconds = Duration.between(cursor, segmentEnd).getSeconds();
            if (seconds > 0) {
                double years = (double) seconds / (365.25 * 24 * 3600);
                double rate = annualRate.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP).doubleValue();
                factor *= Math.pow(1.0 + rate, years);
            }

            cursor = segmentEnd;
        }

        return BigDecimal.valueOf((factor - 1.0) * 100).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveEcbRate(
            LocalDate date,
            NavigableMap<LocalDate, BigDecimal> historicalRates,
            BigDecimal fallbackAnnualRate) {
        if (historicalRates == null || historicalRates.isEmpty()) {
            return fallbackAnnualRate;
        }

        var rateEntry = historicalRates.floorEntry(date);
        if (rateEntry != null) {
            return rateEntry.getValue();
        }

        return historicalRates.firstEntry().getValue();
    }
}
