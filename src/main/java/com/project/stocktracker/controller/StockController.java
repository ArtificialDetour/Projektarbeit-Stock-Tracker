package com.project.stocktracker.controller;

import com.project.stocktracker.model.DailyQuote;
import com.project.stocktracker.model.StockSummary;
import com.project.stocktracker.service.StockService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * REST endpoints for raw market data and FX rates.
 */
@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    /**
     * Returns a market quote for a symbol.
     */
    @GetMapping("/quote/{symbol:.+}")
    public StockSummary getQuote(@PathVariable String symbol) {
        return stockService.getQuote(symbol.toUpperCase(Locale.ROOT));
    }

    /**
     * Returns historical quote data for a symbol.
     */
    @GetMapping("/history/{symbol:.+}")
    public List<DailyQuote> getHistory(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1") int years,
            @RequestParam(required = false) String range) {
        var sym = symbol.toUpperCase(Locale.ROOT);
        if (range != null) {
            return switch (range.toLowerCase(Locale.ROOT)) {
                case "1d"  -> stockService.getHistoricalDataForRange(sym, "5m",  "1d");
                case "1w"  -> stockService.getHistoricalDataForRange(sym, "1h",  "5d");
                case "1m"  -> stockService.getHistoricalDataForRange(sym, "1d",  "1mo");
                case "3m"  -> stockService.getHistoricalDataForRange(sym, "1d",  "3mo");
                case "1y"  -> stockService.getHistoricalDataForRange(sym, "1wk", "1y");
                case "all" -> stockService.getHistoricalDataForRange(sym, "1mo", "5y");
                default    -> stockService.getHistoricalData(sym, years);
            };
        }
        return stockService.getHistoricalData(sym, years);
    }

    /**
     * Returns an exchange rate for a Yahoo Finance currency pair.
     */
    @GetMapping("/fx")
    public Map<String, Object> getFxRate(@RequestParam String pair) {
        var rate = stockService.getFxRate(pair);
        return Map.of("pair", pair, "rate", rate);
    }
}
