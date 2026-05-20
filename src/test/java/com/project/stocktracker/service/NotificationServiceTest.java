package com.project.stocktracker.service;

import com.project.stocktracker.dto.HoldingDto;
import com.project.stocktracker.entity.Notification;
import com.project.stocktracker.entity.User;
import com.project.stocktracker.entity.UserSettings;
import com.project.stocktracker.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock PortfolioService       portfolioService;
    @Mock UserService            userService;

    @InjectMocks NotificationService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@example.com");
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private UserSettings settingsWithAlerts(boolean enabled) {
        var s = new UserSettings();
        s.setPriceAlerts(enabled);
        return s;
    }

    private HoldingDto holding(String symbol, String name, String perf, String weekly) {
        return new HoldingDto(
                symbol, name,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ONE,
                new BigDecimal(perf),
                weekly == null ? null : new BigDecimal(weekly),
                null
        );
    }

    // Notification creation flow.
    @Nested
    @DisplayName("createNotification")
    class CreateNotification {

        @Test
        @DisplayName("Saves notification with correct fields")
        void create_savesCorrectFields() {
            var captor = ArgumentCaptor.forClass(Notification.class);

            service.createNotification(user, "Titel", "Nachricht", "icon_name", "primary");

            verify(notificationRepository).save(captor.capture());
            var saved = captor.getValue();
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getTitle()).isEqualTo("Titel");
            assertThat(saved.getMessage()).isEqualTo("Nachricht");
            assertThat(saved.getIcon()).isEqualTo("icon_name");
            assertThat(saved.getIconStyle()).isEqualTo("primary");
            assertThat(saved.isRead()).isFalse();
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("New notification is unread by default")
        void create_isUnread() {
            var captor = ArgumentCaptor.forClass(Notification.class);

            service.createNotification(user, "T", "M", "i", "s");

            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().isRead()).isFalse();
        }
    }

    // Notification retrieval flow.
    @Nested
    @DisplayName("getNotifications & timeLabel")
    class GetNotifications {

        private Notification notification(String title, LocalDateTime createdAt) {
            var n = new Notification();
            n.setUser(user);
            n.setTitle(title);
            n.setMessage("msg");
            n.setIcon("icon");
            n.setIconStyle("primary");
            n.setRead(false);
            n.setCreatedAt(createdAt);
            return n;
        }

        @Test
        @DisplayName("Returns empty list when no notifications are available")
        void getNotifications_empty() {
            when(notificationRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of());

            assertThat(service.getNotifications(user)).isEmpty();
        }

        @Test
        @DisplayName("Title is correctly mapped to DTO")
        void getNotifications_mapsTitle() {
            when(notificationRepository.findByUserOrderByCreatedAtDesc(user))
                    .thenReturn(List.of(notification("Test", LocalDateTime.now())));

            var result = service.getNotifications(user);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).title()).isEqualTo("Test");
        }

        @Test
        @DisplayName("timeLabel under 2 minutes returns Just Now")
        void timeLabel_justNow() {
            when(notificationRepository.findByUserOrderByCreatedAtDesc(user))
                    .thenReturn(List.of(notification("T", LocalDateTime.now().minusSeconds(30))));

            assertThat(service.getNotifications(user).get(0).timeLabel()).isEqualTo("Just Now");
        }

        @Test
        @DisplayName("timeLabel 30 minutes returns 30m ago")
        void timeLabel_minutes() {
            when(notificationRepository.findByUserOrderByCreatedAtDesc(user))
                    .thenReturn(List.of(notification("T", LocalDateTime.now().minusMinutes(30))));

            assertThat(service.getNotifications(user).get(0).timeLabel()).isEqualTo("30m ago");
        }

        @Test
        @DisplayName("timeLabel 5 hours returns 5h ago")
        void timeLabel_hours() {
            when(notificationRepository.findByUserOrderByCreatedAtDesc(user))
                    .thenReturn(List.of(notification("T", LocalDateTime.now().minusHours(5))));

            assertThat(service.getNotifications(user).get(0).timeLabel()).isEqualTo("5h ago");
        }

        @Test
        @DisplayName("timeLabel 3 days returns 3d ago")
        void timeLabel_days() {
            when(notificationRepository.findByUserOrderByCreatedAtDesc(user))
                    .thenReturn(List.of(notification("T", LocalDateTime.now().minusDays(3))));

            assertThat(service.getNotifications(user).get(0).timeLabel()).isEqualTo("3d ago");
        }
    }

    // Notification state flow.
    @Nested
    @DisplayName("countUnread / markAllRead / deleteAll")
    class Crud {

        @Test
        @DisplayName("countUnread delegates to repository")
        void countUnread_delegatesToRepository() {
            when(notificationRepository.countByUserAndReadFalse(user)).thenReturn(3L);

            assertThat(service.countUnread(user)).isEqualTo(3L);
        }

        @Test
        @DisplayName("markAllRead calls repository method")
        void markAllRead_callsRepository() {
            service.markAllRead(user);

            verify(notificationRepository).markAllReadByUser(user);
        }

        @Test
        @DisplayName("deleteAll calls repository method")
        void deleteAll_callsRepository() {
            service.deleteAll(user);

            verify(notificationRepository).deleteByUser(user);
        }
    }

    // Startup notification flow.
    @Nested
    @DisplayName("Startup Notifications")
    class StartupNotifications {

        @Test
        @DisplayName("No notification when priceAlerts disabled")
        void noAlertsWhenDisabled() {
            when(userService.getUserSettings(user)).thenReturn(settingsWithAlerts(false));

            service.sendStartupPerformanceNotifications(user);

            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("No notification when no holdings available")
        void noAlertsWhenNoHoldings() {
            when(userService.getUserSettings(user)).thenReturn(settingsWithAlerts(true));
            when(portfolioService.getHoldings(user)).thenReturn(List.of());

            service.sendStartupPerformanceNotifications(user);

            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Best and worst performer are saved as notifications")
        void bestAndWorstPerformerNotified() {
            when(userService.getUserSettings(user)).thenReturn(settingsWithAlerts(true));
            when(portfolioService.getHoldings(user)).thenReturn(List.of(
                    holding("AAPL", "Apple", "20.5", null),
                    holding("TSLA", "Tesla", "-5.0", null)
            ));

            service.sendStartupPerformanceNotifications(user);

            var captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository, atLeast(2)).save(captor.capture());
            var titles = captor.getAllValues().stream().map(Notification::getTitle).toList();
            assertThat(titles).contains("Best Performer", "Worst Performer");
        }

        @Test
        @DisplayName("Best Performer contains correct asset name")
        void bestPerformerNotification_containsAssetName() {
            when(userService.getUserSettings(user)).thenReturn(settingsWithAlerts(true));
            when(portfolioService.getHoldings(user)).thenReturn(List.of(
                    holding("AAPL", "Apple Inc", "30.0", null),
                    holding("TSLA", "Tesla", "5.0", null)
            ));

            service.sendStartupPerformanceNotifications(user);

            var captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository, atLeast(1)).save(captor.capture());
            var best = captor.getAllValues().stream()
                    .filter(n -> "Best Performer".equals(n.getTitle()))
                    .findFirst().orElseThrow();
            assertThat(best.getMessage()).contains("Apple Inc");
        }

        @Test
        @DisplayName("Weekly highlights are only created when weeklyChangePercent available")
        void weeklyHighlights_onlyWhenDataAvailable() {
            when(userService.getUserSettings(user)).thenReturn(settingsWithAlerts(true));
            when(portfolioService.getHoldings(user)).thenReturn(List.of(
                    holding("AAPL", "Apple", "10.0", "3.5"),
                    holding("TSLA", "Tesla", "5.0", "-2.0")
            ));

            service.sendStartupPerformanceNotifications(user);

            var captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository, atLeast(1)).save(captor.capture());
            var titles = captor.getAllValues().stream().map(Notification::getTitle).toList();
            assertThat(titles).contains("Weekly Highlight", "Weekly Low");
        }

        @Test
        @DisplayName("Weekly Highlight uses Drop-Text when the best weekly value is negative")
        void weeklyHighlight_usesDropTextWhenBestValueIsNegative() {
            when(userService.getUserSettings(user)).thenReturn(settingsWithAlerts(true));
            when(portfolioService.getHoldings(user)).thenReturn(List.of(
                    holding("AAPL", "Apple", "10.0", "-2.0"),
                    holding("TSLA", "Tesla", "5.0", "-5.0")
            ));

            service.sendStartupPerformanceNotifications(user);

            var captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository, atLeast(1)).save(captor.capture());
            var highlight = captor.getAllValues().stream()
                    .filter(n -> "Weekly Highlight".equals(n.getTitle()))
                    .findFirst().orElseThrow();
            assertThat(highlight.getMessage()).contains("a drop of 2.0%");
            assertThat(highlight.getMessage()).doesNotContain("gain of -");
        }

        @Test
        @DisplayName("Weekly Low uses Gain-Text when the worst weekly value is positive")
        void weeklyLow_usesGainTextWhenWorstValueIsPositive() {
            when(userService.getUserSettings(user)).thenReturn(settingsWithAlerts(true));
            when(portfolioService.getHoldings(user)).thenReturn(List.of(
                    holding("AAPL", "Apple", "10.0", "5.0"),
                    holding("TSLA", "Tesla", "5.0", "2.0")
            ));

            service.sendStartupPerformanceNotifications(user);

            var captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository, atLeast(1)).save(captor.capture());
            var low = captor.getAllValues().stream()
                    .filter(n -> "Weekly Low".equals(n.getTitle()))
                    .findFirst().orElseThrow();
            assertThat(low.getMessage()).contains("a gain of 2.0%");
            assertThat(low.getMessage()).doesNotContain("drop this week of 2.0%");
        }

        @Test
        @DisplayName("No weekly highlights when weeklyChangePercent is null everywhere")
        void weeklyHighlights_skippedWhenNoWeeklyData() {
            when(userService.getUserSettings(user)).thenReturn(settingsWithAlerts(true));
            when(portfolioService.getHoldings(user)).thenReturn(List.of(
                    holding("AAPL", "Apple", "10.0", null),
                    holding("TSLA", "Tesla", "5.0", null)
            ));

            service.sendStartupPerformanceNotifications(user);

            var captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository, atLeast(1)).save(captor.capture());
            var titles = captor.getAllValues().stream().map(Notification::getTitle).toList();
            assertThat(titles).doesNotContain("Weekly Highlight", "Weekly Low");
        }
    }
}
