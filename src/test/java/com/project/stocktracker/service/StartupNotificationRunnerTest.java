package com.project.stocktracker.service;

import com.project.stocktracker.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartupNotificationRunnerTest {

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private StartupNotificationRunner runner;

    @Test
    @DisplayName("Should send notifications for all users on startup")
    void run_sendsNotifications() throws Exception {
        User u1 = new User();
        u1.setEmail("u1@example.com");
        User u2 = new User();
        u2.setEmail("u2@example.com");

        when(userService.findAllUsers()).thenReturn(List.of(u1, u2));

        runner.run();

        verify(notificationService).sendStartupPerformanceNotifications(u1);
        verify(notificationService).sendStartupPerformanceNotifications(u2);
    }

    @Test
    @DisplayName("Should continue if one user fails")
    void run_continuesOnFailure() throws Exception {
        User u1 = new User();
        u1.setEmail("u1@example.com");
        User u2 = new User();
        u2.setEmail("u2@example.com");

        when(userService.findAllUsers()).thenReturn(List.of(u1, u2));
        doThrow(new RuntimeException("Test exception")).when(notificationService).sendStartupPerformanceNotifications(u1);

        runner.run();

        verify(notificationService).sendStartupPerformanceNotifications(u1);
        verify(notificationService).sendStartupPerformanceNotifications(u2);
    }
}
