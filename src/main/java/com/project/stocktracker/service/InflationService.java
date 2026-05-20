package com.project.stocktracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Loads German monthly HICP inflation rates for benchmark charts.
 */
@Service
public class InflationService {

    private static final Logger log = LoggerFactory.getLogger(InflationService.class);

    private static final String EUROSTAT_URL =
            "https://ec.europa.eu/eurostat/api/dissemination/statistics/1.0/data/prc_hicp_manr" +
            "?geo=DE&coicop=CP00&freq=M&format=JSON&sinceTimePeriod=2005-01";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    private List<Map<String, Object>> cachedRates = null;
    private LocalDateTime lastFetched = null;

    /**
     * Returns cached or fetched monthly inflation rates.
     */
    public synchronized List<Map<String, Object>> getMonthlyRates() {
        if (cachedRates != null && lastFetched != null
                && lastFetched.isAfter(LocalDateTime.now().minusHours(24))) {
            return cachedRates;
        }
        try {
            var root = mapper.readTree(fetchRatesJson());

            var timeCategory = root.path("dimension").path("time").path("category");
            var timeIndex    = timeCategory.path("index");
            var values       = root.path("value");

            // Eurostat stores values by numeric index; keep months sorted for chart rendering.
            var rateMap = new TreeMap<String, Double>();
            timeIndex.fields().forEachRemaining(entry -> {
                var month     = entry.getKey();
                if (month.length() == 7 && month.charAt(4) == 'M') {
                    month = month.substring(0, 4) + "-" + month.substring(5);
                }
                var idx       = entry.getValue().asInt();
                var valueNode = values.path(String.valueOf(idx));
                if (!valueNode.isMissingNode() && !valueNode.isNull()) {
                    rateMap.put(month, valueNode.asDouble());
                }
            });

            cachedRates = toList(rateMap);
            lastFetched = LocalDateTime.now();
            log.info("Fetched {} Eurostat HICP monthly rates for Germany", rateMap.size());
            return cachedRates;

        } catch (Exception e) {
            log.warn("Failed to fetch Eurostat HICP rates: {}", e.getMessage());
            if (cachedRates != null) return cachedRates;
            log.info("Using hardcoded HICP fallback rates");
            return toList(buildFallback());
        }
    }

    /**
     * Fetches raw Eurostat inflation JSON.
     */
    protected String fetchRatesJson() throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(EUROSTAT_URL))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .GET().build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    private List<Map<String, Object>> toList(TreeMap<String, Double> map) {
        var list = new ArrayList<Map<String, Object>>(map.size());
        map.forEach((month, rate) -> list.add(Map.of("month", month, "rate", rate)));
        return list;
    }

    // Approximate annual German HICP rates used only when Eurostat and cache are unavailable.
    private TreeMap<String, Double> buildFallback() {
        var map = new TreeMap<String, Double>();
        double[] annualRates = {
            /* 2005 */ 1.9, /* 2006 */ 1.8, /* 2007 */ 2.3, /* 2008 */ 2.8,
            /* 2009 */ 0.2, /* 2010 */ 1.2, /* 2011 */ 2.5, /* 2012 */ 2.1,
            /* 2013 */ 1.6, /* 2014 */ 0.8, /* 2015 */ 0.7, /* 2016 */ 0.4,
            /* 2017 */ 1.7, /* 2018 */ 1.9, /* 2019 */ 1.4, /* 2020 */ 0.5,
            /* 2021 */ 3.1, /* 2022 */ 8.7, /* 2023 */ 6.0, /* 2024 */ 2.5,
            /* 2025 */ 2.3
        };
        int year = 2005;
        for (double rate : annualRates) {
            for (int m = 1; m <= 12; m++) {
                map.put(String.format("%d-%02d", year, m), rate);
            }
            year++;
        }
        return map;
    }
}
