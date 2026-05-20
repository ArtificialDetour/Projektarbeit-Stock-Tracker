package com.project.stocktracker.controller;

import com.project.stocktracker.dto.AssetQuoteDto;
import com.project.stocktracker.dto.AssetSearchResultDto;
import com.project.stocktracker.dto.BuyAssetRequestDto;
import com.project.stocktracker.dto.SellAssetRequestDto;
import com.project.stocktracker.dto.HoldingDto;
import com.project.stocktracker.dto.EcbRateDto;
import com.project.stocktracker.dto.TransactionResponseDto;
import com.project.stocktracker.entity.User;
import com.project.stocktracker.exception.PriceChangedException;
import com.project.stocktracker.service.EcbService;
import com.project.stocktracker.service.PortfolioService;
import com.project.stocktracker.service.StockService;
import com.project.stocktracker.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for asset search, trading, holdings, and portfolio chart data.
 */
@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final StockService stockService;
    private final TransactionService transactionService;
    private final PortfolioService portfolioService;
    private final EcbService ecbService;

    public AssetController(StockService stockService, TransactionService transactionService, PortfolioService portfolioService, EcbService ecbService) {
        this.stockService      = stockService;
        this.transactionService = transactionService;
        this.portfolioService = portfolioService;
        this.ecbService = ecbService;
    }

    /**
     * Searches tradable assets by query text.
     */
    @GetMapping("/search")
    public List<AssetSearchResultDto> search(@RequestParam String q) {
        if (q == null || q.isBlank() || q.length() < 2) return List.of();
        return stockService.searchAssets(q.trim());
    }

    /**
     * Returns a normalized quote for one symbol.
     */
    @GetMapping("/quote/{symbol:.+}")
    public AssetQuoteDto quote(@PathVariable String symbol) {
        return stockService.getAssetQuote(symbol.toUpperCase());
    }

    /**
     * Returns all transactions for the authenticated user.
     */
    @GetMapping("/transactions")
    public List<TransactionResponseDto> transactions(@AuthenticationPrincipal User user) {
        if (user == null) return List.of();
        return transactionService.getTransactions(user);
    }

    /**
     * Returns transactions for one symbol.
     */
    @GetMapping("/transactions/{symbol:.+}")
    public List<TransactionResponseDto> transactionsBySymbol(@AuthenticationPrincipal User user,
                                                              @PathVariable String symbol) {
        if (user == null) return List.of();
        return transactionService.getTransactionsBySymbol(user, symbol.toUpperCase());
    }

    /**
     * Returns current holdings for the authenticated user.
     */
    @GetMapping("/holdings")
    public List<HoldingDto> holdings(@AuthenticationPrincipal User user) {
        if (user == null) return List.of();
        return portfolioService.getHoldings(user);
    }

    /**
     * Returns percentage chart data for the selected range.
     */
    @GetMapping("/portfolio-chart")
    public List<Map<String, Object>> portfolioChart(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "all") String range,
            @RequestParam(required = false) String symbol) {
        if (user == null) return List.of();
        return portfolioService.getPortfolioChart(user, range, symbol);
    }

    /**
     * Creates a buy transaction or returns a confirmation conflict when the price moved.
     */
    @PostMapping("/buy")
    public ResponseEntity<?> buy(@AuthenticationPrincipal User user,
                                  @RequestBody BuyAssetRequestDto req) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            TransactionResponseDto result = transactionService.buyAsset(user, req);
            return ResponseEntity.ok(result);
        } catch (PriceChangedException ex) {
            // The client uses 409 to reopen the confirmation flow with the latest market price.
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("currentPrice", ex.getCurrentPrice()));
        }
    }

    /**
     * Creates a sell transaction or returns a confirmation conflict when the price moved.
     */
    @PostMapping("/sell")
    public ResponseEntity<?> sell(@AuthenticationPrincipal User user,
                                   @RequestBody SellAssetRequestDto req) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            TransactionResponseDto result = transactionService.sellAsset(user, req);
            return ResponseEntity.ok(result);
        } catch (PriceChangedException ex) {
            // The client uses 409 to reopen the confirmation flow with the latest market price.
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("currentPrice", ex.getCurrentPrice()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * Returns the current ECB benchmark rate.
     */
    @GetMapping("/ecb-rate")
    public EcbRateDto getEcbRate() {
        return new EcbRateDto(ecbService.getCurrentEcbRate(), java.time.LocalDateTime.now());
    }
}
