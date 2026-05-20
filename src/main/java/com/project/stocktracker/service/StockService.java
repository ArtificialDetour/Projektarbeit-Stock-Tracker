package com.project.stocktracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stocktracker.dto.AssetQuoteDto;
import com.project.stocktracker.dto.AssetSearchResultDto;
import com.project.stocktracker.model.DailyQuote;
import com.project.stocktracker.model.StockSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Retrieves market quotes, historical prices, asset search results, and FX rates.
 */
@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);
    private static final String CHART_BASE  = "https://query2.finance.yahoo.com/v8/finance/chart/";
    private static final String SEARCH_BASE = "https://query1.finance.yahoo.com/v1/finance/search";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    // FX cache key example: "USDEUR"; rates are short-lived because quotes are shown live.
    private static final long FX_CACHE_TTL_MS = 5 * 60 * 1000;
    private record CachedRate(BigDecimal rate, long timestamp) {}
    private final ConcurrentHashMap<String, CachedRate> fxCache = new ConcurrentHashMap<>();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Fetches a real-time quote for the given symbol and computes the intraday
     * change percent relative to the previous close. Returns a zero-valued
     * fallback if Yahoo Finance is unreachable.
     */
    public StockSummary getQuote(String symbol) {
        try {
            var meta = fetchMeta(symbol, "1d", "1d");
            var price = decimal(meta.get("regularMarketPrice"));
            var prevClose = decimal(meta.get("chartPreviousClose"));
            var changePercent = BigDecimal.ZERO;
            if (prevClose.compareTo(BigDecimal.ZERO) != 0) {
                changePercent = price.subtract(prevClose)
                        .divide(prevClose, 6, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            var volume = meta.has("regularMarketVolume") ? meta.get("regularMarketVolume").asLong() : 0L;
            var name = meta.has("longName") ? meta.get("longName").asText() : symbol;
            return new StockSummary(symbol, name, price, changePercent, volume);
        } catch (Exception e) {
            log.warn("Failed to fetch quote for {}: {}", symbol, e.getMessage());
            return fallbackSummary(symbol);
        }
    }

    /**
     * Fetches monthly OHLCV data for the given symbol over the requested number
     * of years. Entries with a null close price are skipped. Returns an empty
     * list if the request fails.
     */
    public List<DailyQuote> getHistoricalData(String symbol, int years) {
        try {
            return parseHistoricalJson(fetchJson(symbol, "1mo", years + "y"));
        } catch (Exception e) {
            log.warn("Failed to fetch history for {}: {}", symbol, e.getMessage());
            return List.of();
        }
    }

    /**
     * Fetches OHLCV data for the given symbol using an explicit Yahoo Finance
     * interval and range string (e.g. interval="1d", range="1mo"). Returns an
     * empty list if the request fails.
     */
    public List<DailyQuote> getHistoricalDataForRange(String symbol, String interval, String yahooRange) {
        try {
            return parseHistoricalJson(fetchJson(symbol, interval, yahooRange));
        } catch (Exception e) {
            log.warn("Failed to fetch history for {} ({}): {}", symbol, yahooRange, e.getMessage());
            return List.of();
        }
    }

    private List<DailyQuote> parseHistoricalJson(JsonNode root) {
        var result = root.path("chart").path("result");
        if (!result.isArray() || result.isEmpty()) return List.of();

        var item = result.get(0);
        var timestamps = item.path("timestamp");
        var quoteNodes = item.path("indicators").path("quote");
        if (!quoteNodes.isArray() || quoteNodes.isEmpty()) return List.of();

        var q = quoteNodes.get(0);
        var opens   = q.path("open");
        var closes  = q.path("close");
        var highs   = q.path("high");
        var lows    = q.path("low");
        var volumes = q.path("volume");

        var list = new ArrayList<DailyQuote>();
        for (int i = 0; i < timestamps.size(); i++) {
            var closeNode = closes.size() > i ? closes.get(i) : null;
            if (closeNode == null || closeNode.isNull()) continue;
            var date = Instant.ofEpochSecond(timestamps.get(i).asLong())
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
            list.add(new DailyQuote(
                    date,
                    opens.size()   > i ? decimal(opens.get(i))   : BigDecimal.ZERO,
                    decimal(closeNode),
                    highs.size()   > i ? decimal(highs.get(i))   : BigDecimal.ZERO,
                    lows.size()    > i ? decimal(lows.get(i))    : BigDecimal.ZERO,
                    volumes.size() > i ? volumes.get(i).asLong() : 0L
            ));
        }
        return list;
    }

    /**
     * Fetches quotes for multiple symbols sequentially and returns them as a map
     * keyed by ticker symbol. Each symbol falls back independently on failure.
     */
    public Map<String, StockSummary> getMultipleQuotes(List<String> symbols) {
        var result = new HashMap<String, StockSummary>();
        symbols.forEach(s -> result.put(s, getQuote(s)));
        return result;
    }

    /**
     * Returns the current exchange rate for the given currency pair.
     * Yahoo Finance symbol format appends "=X" to the pair (e.g. USDEUR -> USDEUR=X).
     * Results are cached for 5 minutes to avoid excessive API calls.
     * Falls back to 1.0 (no conversion) if the request fails.
     */
    public BigDecimal getFxRate(String pair) {
        var cached = fxCache.get(pair);
        if (cached != null && (System.currentTimeMillis() - cached.timestamp()) < FX_CACHE_TTL_MS) {
            return cached.rate();
        }
        try {
            var meta = fetchMeta(pair + "=X", "1d", "1d");
            var rate = decimal(meta.get("regularMarketPrice"));
            fxCache.put(pair, new CachedRate(rate, System.currentTimeMillis()));
            return rate;
        } catch (Exception e) {
            log.warn("Failed to fetch FX rate for {}: {}", pair, e.getMessage());
            return cached != null ? cached.rate() : BigDecimal.ONE;
        }
    }

    /**
     * Returns the FX rate to convert from the given currency to EUR.
     * If the currency is already EUR, returns 1.
     */
    public BigDecimal getFxRateToEur(String currency) {
        if (currency == null || currency.equalsIgnoreCase("EUR")) {
            return BigDecimal.ONE;
        }
        return getFxRate(currency.toUpperCase() + "EUR");
    }

    /**
     * Returns the currency of the given symbol from Yahoo Finance meta data.
     * Falls back to "USD" if unknown.
     */
    public String getAssetCurrency(String symbol) {
        try {
            var meta = fetchMeta(symbol, "1d", "1d");
            return meta.has("currency") ? meta.get("currency").asText("USD") : "USD";
        } catch (Exception e) {
            return "USD";
        }
    }

    /**
     * Fetches the chart JSON and extracts the "meta" node from the first result.
     * Throws if the result array is empty or missing.
     */
    private JsonNode fetchMeta(String symbol, String interval, String range) throws Exception {
        var root = fetchJson(symbol, interval, range);
        var result = root.path("chart").path("result");
        if (!result.isArray() || result.isEmpty()) {
            throw new IllegalStateException("No result for symbol: " + symbol);
        }
        return result.get(0).path("meta");
    }

    /**
     * Sends a GET request to the Yahoo Finance v8 chart endpoint and parses
     * the response body as JSON. Throws on non-200 status codes.
     */
    private JsonNode fetchJson(String symbol, String interval, String range) throws Exception {
        var encoded = URLEncoder.encode(symbol, StandardCharsets.UTF_8);
        var url = CHART_BASE + encoded + "?interval=" + interval + "&range=" + range;
        try {
            return mapper.readTree(fetchJsonFromUrl(url));
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e.getMessage() + " for " + symbol);
        }
    }

    private String fetchJsonFromUrl(String url) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    /**
     * Fetches chart data for the given range and returns the first non-null close
     * price, representing the portfolio value at the start of that period.
     * Returns BigDecimal.ZERO if the request fails or no data is available.
     */
    public BigDecimal getPriceAtRangeStart(String symbol, String interval, String range) {
        try {
            var root = fetchJson(symbol, interval, range);
            var result = root.path("chart").path("result");
            if (!result.isArray() || result.isEmpty()) return BigDecimal.ZERO;
            var closes = result.get(0).path("indicators").path("quote").get(0).path("close");
            for (int i = 0; i < closes.size(); i++) {
                var node = closes.get(i);
                if (!node.isNull()) return decimal(node);
            }
            return BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Failed to get range start price for {}: {}", symbol, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Safely converts a JsonNode to BigDecimal. Returns BigDecimal.ZERO
     * for null, missing, or unparseable nodes.
     */
    private BigDecimal decimal(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(node.asText());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Searches Yahoo Finance for assets matching the query.
     * Returns up to 8 results with symbol, name, and exchange.
     */
    public List<AssetSearchResultDto> searchAssets(String query) {
        try {
            var encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            var url = SEARCH_BASE + "?q=" + encoded + "&quotesCount=8&newsCount=0&enableFuzzyQuery=false";
            String json;
            try {
                json = fetchJsonFromUrl(url);
            } catch (IllegalStateException e) {
                return List.of();
            }

            var root = mapper.readTree(json);
            var quotes = root.path("quotes");
            if (!quotes.isArray()) return List.of();

            var results = new ArrayList<AssetSearchResultDto>();
            for (int i = 0; i < quotes.size(); i++) {
                var q        = quotes.get(i);
                var symbol   = q.path("symbol").asText("");
                var name     = q.has("longname") ? q.path("longname").asText("")
                             : q.path("shortname").asText("");
                var exchange = q.path("exchDisp").asText(q.path("exchange").asText(""));
                if (!symbol.isEmpty()) results.add(new AssetSearchResultDto(symbol, name, exchange));
            }
            return results;
        } catch (Exception e) {
            log.warn("Asset search failed for '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    /**
     * Returns a live quote as AssetQuoteDto (symbol, name, price in EUR, currency="EUR").
     * Non-EUR prices are automatically converted using the current FX rate.
     * Falls back to zero price if Yahoo Finance is unreachable.
     */
    public AssetQuoteDto getAssetQuote(String symbol) {
        try {
            var meta = fetchMeta(symbol, "1d", "1d");
            var price        = decimal(meta.get("regularMarketPrice"));
            var origCurrency = meta.has("currency") ? meta.get("currency").asText("USD") : "USD";
            var name         = meta.has("longName")  ? meta.get("longName").asText(symbol) : symbol;

            // Normalize prices to EUR so portfolio calculations can aggregate mixed markets.
            var fxRate   = getFxRateToEur(origCurrency);
            var eurPrice = price.multiply(fxRate).setScale(6, RoundingMode.HALF_UP);

            return new AssetQuoteDto(symbol, name, eurPrice, "EUR");
        } catch (Exception e) {
            log.warn("Failed to fetch asset quote for {}: {}", symbol, e.getMessage());
            return new AssetQuoteDto(symbol, symbol, BigDecimal.ZERO, "EUR");
        }
    }

    /**
     * Returns a zero-valued StockSummary used as a safe fallback when
     * a Yahoo Finance request fails, preventing a 500 response.
     */
    private StockSummary fallbackSummary(String symbol) {
        return new StockSummary(symbol, symbol, BigDecimal.ZERO, BigDecimal.ZERO, 0L);
    }

}
