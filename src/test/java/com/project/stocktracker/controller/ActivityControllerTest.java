package com.project.stocktracker.controller;

import com.project.stocktracker.dto.ActivityDto;
import com.project.stocktracker.entity.User;
import com.project.stocktracker.service.ActivityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ActivityControllerTest {

    @Mock
    private ActivityService activityService;

    @InjectMocks
    private ActivityController controller;

    private MockMvc mockMvc;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@example.com");

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        var auth = new UsernamePasswordAuthenticationToken(mockUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should return list of activities for authenticated user")
    void getActivities_returnsList() throws Exception {
        ActivityDto dto = new ActivityDto(1L, "Title", "Desc", "TRADE", "1m ago");
        when(activityService.getUserActivities(any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Title"))
                .andExpect(jsonPath("$[0].type").value("TRADE"));
    }

    @Test
    @DisplayName("Should return empty list for unauthenticated user")
    void getActivities_unauthenticated_returnsEmpty() throws Exception {
        SecurityContextHolder.clearContext();
        
        mockMvc.perform(get("/api/activities"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("Should log export activity for authenticated user")
    void logExport_logsActivity() throws Exception {
        mockMvc.perform(post("/api/activities/log-export"))
                .andExpect(status().isOk());

        verify(activityService, times(1)).logActivity(eq(mockUser), eq("EXPORT"), eq("Data Exported"), any());
    }
}
