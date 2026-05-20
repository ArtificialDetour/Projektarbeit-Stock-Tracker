package com.project.stocktracker.controller;

import com.project.stocktracker.service.InflationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST endpoint for monthly inflation benchmark data.
 */
@RestController
@RequestMapping("/api/inflation")
public class InflationController {

    private final InflationService inflationService;

    public InflationController(InflationService inflationService) {
        this.inflationService = inflationService;
    }

    /**
     * Returns monthly inflation rates used by portfolio charts.
     */
    @GetMapping
    public List<Map<String, Object>> getRates() {
        return inflationService.getMonthlyRates();
    }
}
