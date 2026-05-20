package com.project.stocktracker.service;

import com.project.stocktracker.entity.Activity;
import com.project.stocktracker.entity.User;
import com.project.stocktracker.repository.ActivityRepository;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActivityServiceTest {

    @Mock ActivityRepository activityRepository;

    @InjectMocks ActivityService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@example.com");
        when(activityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Activity activity(String title, String desc, String type, LocalDateTime createdAt) {
        var a = new Activity();
        a.setUser(user);
        a.setTitle(title);
        a.setDescription(desc);
        a.setType(type);
        a.setCreatedAt(createdAt);
        return a;
    }

    // Activity logging flow.
    @Nested
    @DisplayName("logActivity")
    class LogActivity {

        @Test
        @DisplayName("Saves activity with all given fields")
        void log_savesAllFields() {
            var captor = ArgumentCaptor.forClass(Activity.class);

            service.logActivity(user, "TRADE", "Kauf", "Du hast 5 AAPL gekauft.");

            verify(activityRepository).save(captor.capture());
            var saved = captor.getValue();
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getType()).isEqualTo("TRADE");
            assertThat(saved.getTitle()).isEqualTo("Kauf");
            assertThat(saved.getDescription()).isEqualTo("Du hast 5 AAPL gekauft.");
        }

        @Test
        @DisplayName("Repository.save is called exactly once")
        void log_callsSaveOnce() {
            service.logActivity(user, "EXPORT", "Export", "CSV exportiert.");

            verify(activityRepository, times(1)).save(any());
        }
    }

    // Activity retrieval flow.
    @Nested
    @DisplayName("getUserActivities & timeLabel")
    class GetActivities {

        @Test
        @DisplayName("Returns empty list when no activities available")
        void getActivities_empty() {
            when(activityRepository.findTop20ByUserOrderByCreatedAtDescIdDesc(user)).thenReturn(List.of());

            assertThat(service.getUserActivities(user)).isEmpty();
        }

        @Test
        @DisplayName("Title and type are mapped correctly to DTO")
        void getActivities_mapsFields() {
            when(activityRepository.findTop20ByUserOrderByCreatedAtDescIdDesc(user))
                    .thenReturn(List.of(activity("Kauf", "Beschreibung", "TRADE", LocalDateTime.now())));

            var result = service.getUserActivities(user);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).title()).isEqualTo("Kauf");
            assertThat(result.get(0).type()).isEqualTo("TRADE");
        }

        @Test
        @DisplayName("timeLabel under 2 minutes returns Just Now")
        void timeLabel_justNow() {
            when(activityRepository.findTop20ByUserOrderByCreatedAtDescIdDesc(user))
                    .thenReturn(List.of(activity("T", "D", "TRADE", LocalDateTime.now().minusSeconds(30))));

            assertThat(service.getUserActivities(user).get(0).timeLabel()).isEqualTo("Just Now");
        }

        @Test
        @DisplayName("timeLabel 45 minutes returns 45m ago")
        void timeLabel_minutes() {
            when(activityRepository.findTop20ByUserOrderByCreatedAtDescIdDesc(user))
                    .thenReturn(List.of(activity("T", "D", "TRADE", LocalDateTime.now().minusMinutes(45))));

            assertThat(service.getUserActivities(user).get(0).timeLabel()).isEqualTo("45m ago");
        }

        @Test
        @DisplayName("timeLabel 3 hours returns 3h ago")
        void timeLabel_hours() {
            when(activityRepository.findTop20ByUserOrderByCreatedAtDescIdDesc(user))
                    .thenReturn(List.of(activity("T", "D", "TRADE", LocalDateTime.now().minusHours(3))));

            assertThat(service.getUserActivities(user).get(0).timeLabel()).isEqualTo("3h ago");
        }

        @Test
        @DisplayName("timeLabel 2 days returns 2d ago")
        void timeLabel_days() {
            when(activityRepository.findTop20ByUserOrderByCreatedAtDescIdDesc(user))
                    .thenReturn(List.of(activity("T", "D", "TRADE", LocalDateTime.now().minusDays(2))));

            assertThat(service.getUserActivities(user).get(0).timeLabel()).isEqualTo("2d ago");
        }

        @Test
        @DisplayName("timeLabel null returns empty string")
        void timeLabel_null() {
            when(activityRepository.findTop20ByUserOrderByCreatedAtDescIdDesc(user))
                    .thenReturn(List.of(activity("T", "D", "TRADE", null)));

            assertThat(service.getUserActivities(user).get(0).timeLabel()).isEmpty();
        }
    }

    // Activity clearing flow.
    @Nested
    @DisplayName("clearActivities")
    class ClearActivities {

        @Test
        @DisplayName("Delegates delete to repository")
        void clear_callsRepository() {
            service.clearActivities(user);

            verify(activityRepository).deleteByUser(user);
        }

        @Test
        @DisplayName("deleteByUser is called with the correct user")
        void clear_correctUser() {
            var other = new User();
            other.setEmail("other@example.com");

            service.clearActivities(user);

            verify(activityRepository).deleteByUser(user);
            verify(activityRepository, never()).deleteByUser(other);
        }
    }
}
