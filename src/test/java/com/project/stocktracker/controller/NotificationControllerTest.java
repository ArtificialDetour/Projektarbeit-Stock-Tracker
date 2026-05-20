package com.project.stocktracker.controller;

import com.project.stocktracker.dto.NotificationDto;
import com.project.stocktracker.entity.User;
import com.project.stocktracker.service.NotificationService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController controller;

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
    @DisplayName("Should return list of notifications for authenticated user")
    void getNotifications_returnsList() throws Exception {
        NotificationDto dto = new NotificationDto(1L, "Title", "Msg", "icon", "style", false, "1m ago");
        when(notificationService.getNotifications(any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Title"));
    }

    @Test
    @DisplayName("Should return unread notification count for authenticated user")
    void getStatus_returnsUnreadCount() throws Exception {
        when(notificationService.countUnread(any())).thenReturn(5L);

        mockMvc.perform(get("/api/notifications/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(5));
    }

    @Test
    @DisplayName("Should mark all notifications as read and return zero count")
    void markRead_callsServiceAndReturnsZero() throws Exception {
        mockMvc.perform(post("/api/notifications/mark-read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));

        verify(notificationService).markAllRead(any());
    }

    @Test
    @DisplayName("Should delete all notifications for authenticated user")
    void clearAll_callsService() throws Exception {
        mockMvc.perform(delete("/api/notifications"))
                .andExpect(status().isOk());

        verify(notificationService).deleteAll(any());
    }
}
