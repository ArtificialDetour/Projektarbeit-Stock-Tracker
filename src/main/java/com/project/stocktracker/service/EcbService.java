package com.project.stocktracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Loads current and historical ECB deposit facility rates.
 */
@Service
public class EcbService {

    private static final Logger log = Logger.getLogger(EcbService.class.getName());

    // Deposit Facility Rate: banks' overnight deposit rate, used here as a conservative benchmark.
    private static final String ECB_API_URL =
        "https://data-api.ecb.europa.eu/service/data/FM/D.U2.EUR.4F.KR.DFR.LEV" +
        "?format=jsondata&detail=dataonly&lastNObservations=1";

    // Current DFR value used only when the ECB API and cache are unavailable.
    private static final BigDecimal FALLBACK_RATE = new BigDecimal("2.00");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private BigDecimal cachedRate = null;
    private LocalDateTime lastFetched = null;
    private NavigableMap<LocalDate, BigDecimal> cachedHistoricalRates = null;
    private LocalDateTime historicalLastFetched = null;

    public EcbService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns the latest cached or fetched ECB deposit facility rate.
     */
    public synchronized BigDecimal getCurrentEcbRate() {
        if (cachedRate != null && lastFetched != null
                && lastFetched.isAfter(LocalDateTime.now().minusHours(24))) {
            return cachedRate;
        }

        try {
            log.info("Fetching ECB DFR from API...");
            String response = restTemplate.getForObject(ECB_API_URL, String.class);
            JsonNode root = objectMapper.readTree(response);

            JsonNode dataSets = root.path("dataSets");
            if (dataSets.isArray() && !dataSets.isEmpty()) {
                JsonNode series = dataSets.get(0).path("series");
                // ECB series keys can change, so use the first returned series instead of a fixed key.
                JsonNode seriesNode = series.fields().hasNext()
                        ? series.fields().next().getValue()
                        : null;
                if (seriesNode != null) {
                    JsonNode obs = seriesNode.path("observations");
                    // Observation keys are numeric positions; the highest key represents the latest value.
                    String latestKey = null;
                    int latestIdx = -1;
                    var iter = obs.fieldNames();
                    while (iter.hasNext()) {
                        String key = iter.next();
                        try {
                            int idx = Integer.parseInt(key);
                            if (idx > latestIdx) { latestIdx = idx; latestKey = key; }
                        } catch (NumberFormatException ignored) {}
                    }
                    if (latestKey != null) {
                        JsonNode values = obs.path(latestKey);
                        if (values.isArray() && !values.isEmpty()) {
                            double rate = values.get(0).asDouble();
                            cachedRate = BigDecimal.valueOf(rate);
                            lastFetched = LocalDateTime.now();
                            log.info("ECB DFR fetched: " + cachedRate + "%");
                            return cachedRate;
                        }
                    }
                }
            }
            log.warning("Could not parse ECB rate — using fallback.");
        } catch (Exception e) {
            log.severe("Failed to fetch ECB rate: " + e.getMessage());
        }

        return cachedRate != null ? cachedRate : FALLBACK_RATE;
    }

    /**
     * Returns the historical ECB Deposit Facility Rate keyed by the date from which each rate applies.
     */
    public synchronized NavigableMap<LocalDate, BigDecimal> getHistoricalDepositRates() {
        if (cachedHistoricalRates != null && historicalLastFetched != null
                && historicalLastFetched.isAfter(LocalDateTime.now().minusHours(24))) {
            return new TreeMap<>(cachedHistoricalRates);
        }

        try {
            log.info("Fetching historical ECB DFR series from API...");
            String response = restTemplate.getForObject(
                    ECB_API_URL.replace("&detail=dataonly", "").replace("&lastNObservations=1", ""),
                    String.class);
            NavigableMap<LocalDate, BigDecimal> parsedRates = parseHistoricalRates(response);
            if (!parsedRates.isEmpty()) {
                cachedHistoricalRates = parsedRates;
                historicalLastFetched = LocalDateTime.now();
                cachedRate = parsedRates.lastEntry().getValue();
                lastFetched = historicalLastFetched;
                log.info("Historical ECB DFR points fetched: " + parsedRates.size());
                return new TreeMap<>(parsedRates);
            }
            log.warning("Could not parse historical ECB rates - using current fallback rate.");
        } catch (Exception e) {
            log.severe("Failed to fetch historical ECB rates: " + e.getMessage());
        }

        return new TreeMap<>(Map.of(LocalDate.now(), getCurrentEcbRate()));
    }

    private NavigableMap<LocalDate, BigDecimal> parseHistoricalRates(String response) throws Exception {
        NavigableMap<LocalDate, BigDecimal> rates = new TreeMap<>();
        JsonNode root = objectMapper.readTree(response);

        JsonNode timeValues = root.path("structure").path("dimensions").path("observation").path(0).path("values");
        if (!timeValues.isArray() || timeValues.isEmpty()) {
            return rates;
        }

        JsonNode dataSets = root.path("dataSets");
        if (!dataSets.isArray() || dataSets.isEmpty()) {
            return rates;
        }

        JsonNode series = dataSets.get(0).path("series");
        JsonNode seriesNode = series.fields().hasNext()
                ? series.fields().next().getValue()
                : null;
        if (seriesNode == null) {
            return rates;
        }

        JsonNode observations = seriesNode.path("observations");
        var iter = observations.fields();
        while (iter.hasNext()) {
            var entry = iter.next();
            int timeIndex = parseObservationTimeIndex(entry.getKey());
            if (timeIndex < 0 || timeIndex >= timeValues.size()) {
                continue;
            }

            JsonNode values = entry.getValue();
            if (!values.isArray() || values.isEmpty()) {
                continue;
            }

            String dateText = timeValues.get(timeIndex).path("id").asText();
            if (dateText == null || dateText.isBlank()) {
                continue;
            }

            rates.put(LocalDate.parse(dateText), BigDecimal.valueOf(values.get(0).asDouble()));
        }

        return rates;
    }

    private int parseObservationTimeIndex(String observationKey) {
        try {
            String firstDimension = observationKey.split(":", 2)[0];
            return Integer.parseInt(firstDimension);
        } catch (RuntimeException e) {
            return -1;
        }
    }
}
