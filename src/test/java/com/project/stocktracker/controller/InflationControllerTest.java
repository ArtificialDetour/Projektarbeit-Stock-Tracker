package com.project.stocktracker.controller;

import com.project.stocktracker.service.InflationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InflationControllerTest {

    @Mock
    private InflationService inflationService;

    @InjectMocks
    private InflationController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Should return list of monthly inflation rates")
    void getRates_returnsList() throws Exception {
        when(inflationService.getMonthlyRates()).thenReturn(List.of(Map.of("date", "2023-01", "rate", 2.5)));

        mockMvc.perform(get("/api/inflation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2023-01"))
                .andExpect(jsonPath("$[0].rate").value(2.5));
    }
}
