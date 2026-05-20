package com.project.stocktracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EcbServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;

    private EcbService ecbService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ecbService = new EcbService(restTemplate, objectMapper);
    }

    @Test
    @DisplayName("Should return fallback rate when API fails")
    void getCurrentEcbRate_apiFails_returnsFallback() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenThrow(new RuntimeException("API down"));

        BigDecimal rate = ecbService.getCurrentEcbRate();

        assertThat(rate).isEqualByComparingTo(new BigDecimal("2.00"));
    }

    @Test
    @DisplayName("Should parse valid JSON response successfully")
    void getCurrentEcbRate_validResponse_returnsRate() {
        String jsonResponse = """
            {
              "dataSets": [
                {
                  "series": {
                    "0:0:0:0:0:0:0": {
                      "observations": {
                        "0": [3.5]
                      }
                    }
                  }
                }
              ]
            }
            """;
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(jsonResponse);

        BigDecimal rate = ecbService.getCurrentEcbRate();

        assertThat(rate).isEqualByComparingTo(new BigDecimal("3.5"));

        // The second call should reuse the cached ECB rate.
        BigDecimal cachedRate = ecbService.getCurrentEcbRate();
        assertThat(cachedRate).isEqualByComparingTo(new BigDecimal("3.5"));
    }

    @Test
    @DisplayName("Should parse historical ECB rate series")
    void getHistoricalDepositRates_validResponse_returnsDateRateMap() {
        String jsonResponse = """
            {
              "structure": {
                "dimensions": {
                  "observation": [
                    {
                      "values": [
                        { "id": "2024-01-01" },
                        { "id": "2024-06-01" },
                        { "id": "2025-01-01" }
                      ]
                    }
                  ]
                }
              },
              "dataSets": [
                {
                  "series": {
                    "0:0:0:0:0:0:0": {
                      "observations": {
                        "0": [4.0],
                        "1": [3.75],
                        "2": [2.0]
                      }
                    }
                  }
                }
              ]
            }
            """;
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(jsonResponse);

        var rates = ecbService.getHistoricalDepositRates();

        assertThat(rates).hasSize(3);
        assertThat(rates.firstEntry().getValue()).isEqualByComparingTo("4.0");
        assertThat(rates.lastEntry().getValue()).isEqualByComparingTo("2.0");
    }
}
