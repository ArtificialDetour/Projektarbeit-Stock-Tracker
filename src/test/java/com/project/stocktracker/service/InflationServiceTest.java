package com.project.stocktracker.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InflationServiceTest {

    // Minimal Eurostat response fixture.
    private static final String EUROSTAT_JSON = """
            {
              "dimension": {
                "time": {
                  "category": {
                    "index": { "2022-01": 0, "2022-02": 1 }
                  }
                }
              },
              "value": { "0": 5.7, "1": 5.1 }
            }
            """;

    @Spy
    InflationService service;

    // Eurostat JSON parsing flow.
    @Nested
    @DisplayName("JSON-Parsing")
    class JsonParsing {

        @Test
        @DisplayName("Month and rate are correctly extracted from Eurostat-JSON")
        void parsesMonthAndRateCorrectly() throws Exception {
            doReturn(EUROSTAT_JSON).when(service).fetchRatesJson();

            var result = service.getMonthlyRates();

            assertThat(result).hasSize(2);
            assertThat(result.get(0)).containsEntry("month", "2022-01").containsEntry("rate", 5.7);
            assertThat(result.get(1)).containsEntry("month", "2022-02").containsEntry("rate", 5.1);
        }

        @Test
        @DisplayName("Result is sorted chronologically (oldest month first)")
        void resultIsSortedChronologically() throws Exception {
            // The source index order can differ from chronological order.
            var invertedJson = """
                    {
                      "dimension": {
                        "time": { "category": { "index": { "2022-02": 0, "2022-01": 1 } } }
                      },
                      "value": { "0": 5.1, "1": 5.7 }
                    }
                    """;
            doReturn(invertedJson).when(service).fetchRatesJson();

            var result = service.getMonthlyRates();

            assertThat(result.get(0).get("month")).isEqualTo("2022-01");
            assertThat(result.get(1).get("month")).isEqualTo("2022-02");
        }

        @Test
        @DisplayName("Months without values (null) are skipped")
        void nullValuesAreSkipped() throws Exception {
            var jsonWithNull = """
                    {
                      "dimension": {
                        "time": { "category": { "index": { "2022-01": 0, "2022-02": 1 } } }
                      },
                      "value": { "0": 5.7 }
                    }
                    """;
            doReturn(jsonWithNull).when(service).fetchRatesJson();

            var result = service.getMonthlyRates();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("month")).isEqualTo("2022-01");
        }
    }

    // Inflation cache flow.
    @Nested
    @DisplayName("24h-Cache")
    class Cache {

        @Test
        @DisplayName("Second call does not fetch new data (fetchRatesJson called only once)")
        void secondCall_usesCachedResult() throws Exception {
            doReturn(EUROSTAT_JSON).when(service).fetchRatesJson();

            service.getMonthlyRates();
            service.getMonthlyRates();

            verify(service, times(1)).fetchRatesJson();
        }

        @Test
        @DisplayName("Both calls return identical result")
        void cachedResult_isIdentical() throws Exception {
            doReturn(EUROSTAT_JSON).when(service).fetchRatesJson();

            var first  = service.getMonthlyRates();
            var second = service.getMonthlyRates();

            assertThat(first).isSameAs(second);
        }
    }

    // Inflation fallback flow.
    @Nested
    @DisplayName("API error handled gracefully")
    class Fallback {

        @Test
        @DisplayName("Fallback list is returned (not empty)")
        void apiError_returnsFallback() throws Exception {
            doThrow(new RuntimeException("Connection refused")).when(service).fetchRatesJson();

            var result = service.getMonthlyRates();

            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("Fallback months are in YYYY-MM format")
        void fallback_monthFormatIsCorrect() throws Exception {
            doThrow(new RuntimeException("timeout")).when(service).fetchRatesJson();

            var result = service.getMonthlyRates();

            assertThat(result).allSatisfy(entry -> {
                var month = (String) entry.get("month");
                assertThat(month).matches("\\d{4}-\\d{2}");
            });
        }

        @Test
        @DisplayName("Fallback rates are positive")
        void fallback_ratesArePositive() throws Exception {
            doThrow(new RuntimeException("timeout")).when(service).fetchRatesJson();

            var result = service.getMonthlyRates();

            assertThat(result).allSatisfy(entry -> {
                var rate = (Double) entry.get("rate");
                assertThat(rate).isGreaterThan(0.0);
            });
        }

        @Test
        @DisplayName("Fallback contains data for 2022 with increased inflation (>5%)")
        void fallback_2022HasHighInflation() throws Exception {
            doThrow(new RuntimeException("timeout")).when(service).fetchRatesJson();

            var result = service.getMonthlyRates();

            var rate2022 = result.stream()
                    .filter(e -> ((String) e.get("month")).startsWith("2022"))
                    .mapToDouble(e -> (Double) e.get("rate"))
                    .average()
                    .orElse(0.0);

            assertThat(rate2022).isGreaterThan(5.0);
        }

        @Test
        @DisplayName("After error, existing cache is returned")
        void afterError_existingCacheIsPreserved() throws Exception {
            doReturn(EUROSTAT_JSON).when(service).fetchRatesJson();
            var firstResult = service.getMonthlyRates();

            // Force cache expiry through the internal timestamp.
            var field = InflationService.class.getDeclaredField("lastFetched");
            field.setAccessible(true);
            field.set(service, java.time.LocalDateTime.now().minusHours(25));

            doThrow(new RuntimeException("API down")).when(service).fetchRatesJson();
            var secondResult = service.getMonthlyRates();

            assertThat(secondResult).isSameAs(firstResult);
        }
    }
}
