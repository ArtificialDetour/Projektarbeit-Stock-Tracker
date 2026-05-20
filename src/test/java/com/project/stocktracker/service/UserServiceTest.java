package com.project.stocktracker.service;

import com.project.stocktracker.dto.UserRegistrationDto;
import com.project.stocktracker.dto.UserSettingsDto;
import com.project.stocktracker.entity.User;
import com.project.stocktracker.entity.UserSettings;
import com.project.stocktracker.repository.HoldingRepository;
import com.project.stocktracker.repository.NotificationRepository;
import com.project.stocktracker.repository.TransactionRepository;
import com.project.stocktracker.repository.UserRepository;
import com.project.stocktracker.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock UserRepository           userRepository;
    @Mock UserSettingsRepository   userSettingsRepository;
    @Mock HoldingRepository        holdingRepository;
    @Mock TransactionRepository    transactionRepository;
    @Mock NotificationRepository   notificationRepository;
    @Mock ActivityService          activityService;
    @Mock PasswordEncoder          passwordEncoder;

    @InjectMocks UserService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@example.com");
        user.setFirstName("Max");
        user.setLastName("Mustermann");

        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userSettingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "encoded_" + inv.getArgument(0));
    }

    // Registration flow.
    @Nested
    @DisplayName("Registration")
    class Registration {

        private UserRegistrationDto dto(String email, String pw) {
            return new UserRegistrationDto(email, pw, pw, "Max", "Mustermann");
        }

        @Test
        @DisplayName("Successful registration saves user and default settings")
        void register_success_savesUserAndSettings() {
            when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);

            var result = service.registerUser(dto("new@example.com", "secret"));

            assertThat(result.user().getEmail()).isEqualTo("new@example.com");
            verify(userRepository).save(any(User.class));
            verify(userSettingsRepository).save(any(UserSettings.class));
        }

        @Test
        @DisplayName("Password is hashed before storage")
        void register_passwordIsEncoded() {
            when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

            var result = service.registerUser(dto("new@example.com", "secret"));

            assertThat(result.user().getPassword()).startsWith("encoded_");
        }

        @Test
        @DisplayName("Security code is generated and only its hash is stored")
        void register_generatesSecurityCodeAndStoresHash() {
            when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

            var result = service.registerUser(dto("new@example.com", "secret"));

            assertThat(result.securityCode()).matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}");
            assertThat(result.user().getSecurityCodeHash()).startsWith("encoded_");
            assertThat(result.user().getSecurityCodeHash()).isNotEqualTo(result.securityCode());
        }

        @Test
        @DisplayName("Email is normalized before storage (lowercase, trim)")
        void register_emailNormalized() {
            when(userRepository.existsByEmailIgnoreCase("upper@example.com")).thenReturn(false);

            var result = service.registerUser(dto("  UPPER@Example.COM  ", "pw"));

            assertThat(result.user().getEmail()).isEqualTo("upper@example.com");
        }

        @Test
        @DisplayName("Duplicate email throws IllegalArgumentException")
        void register_duplicateEmail_throwsException() {
            when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> service.registerUser(dto("existing@example.com", "pw")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already registered");
        }

        @Test
        @DisplayName("Duplicate email: no user is saved")
        void register_duplicateEmail_noSave() {
            when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

            assertThatThrownBy(() -> service.registerUser(dto("existing@example.com", "pw")));

            verify(userRepository, never()).save(any());
        }
    }

    // Password reset flow.
    @Nested
    @DisplayName("Password Reset")
    class PasswordReset {

        @BeforeEach
        void setSecurityCodeHash() {
            user.setPassword("old_hash");
            user.setSecurityCodeHash("encoded_ABCD2345");
        }

        @Test
        @DisplayName("Correct security code updates password")
        void resetPassword_correctCode_updatesPassword() {
            when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("ABCD2345", "encoded_ABCD2345")).thenReturn(true);

            service.resetPassword("TEST@example.com", "abcd2345", "NewPass123!");

            assertThat(user.getPassword()).isEqualTo("encoded_NewPass123!");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Wrong security code leaves password unchanged")
        void resetPassword_wrongCode_doesNotUpdatePassword() {
            when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("WRONG999", "encoded_ABCD2345")).thenReturn(false);

            assertThatThrownBy(() -> service.resetPassword("test@example.com", "WRONG999", "NewPass123!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Password reset failed");

            assertThat(user.getPassword()).isEqualTo("old_hash");
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Unknown email fails neutrally")
        void resetPassword_unknownEmail_failsNeutrally() {
            when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resetPassword("unknown@example.com", "ABCD2345", "NewPass123!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Password reset failed");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Invalid new password fails neutrally")
        void resetPassword_invalidPassword_failsNeutrally() {
            assertThatThrownBy(() -> service.resetPassword("test@example.com", "ABCD2345", "short"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Password reset failed");

            verify(userRepository, never()).save(any(User.class));
        }
    }

    // User lookup flow.
    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUser {

        @Test
        @DisplayName("Known email returns user")
        void loadUser_knownEmail_returnsUser() {
            when(userRepository.findByMarkedDeletionBefore(any())).thenReturn(List.of());
            when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

            var result = service.loadUserByUsername("test@example.com");

            assertThat(result).isEqualTo(user);
        }

        @Test
        @DisplayName("Unknown email throws UsernameNotFoundException")
        void loadUser_unknownEmail_throwsException() {
            when(userRepository.findByMarkedDeletionBefore(any())).thenReturn(List.of());
            when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.loadUserByUsername("nobody@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class);
        }

        @Test
        @DisplayName("Email is normalized (uppercase)")
        void loadUser_emailNormalized() {
            when(userRepository.findByMarkedDeletionBefore(any())).thenReturn(List.of());
            when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

            service.loadUserByUsername("TEST@EXAMPLE.COM");

            verify(userRepository).findByEmailIgnoreCase("test@example.com");
        }

        @Test
        @DisplayName("Expired accounts are deleted before login")
        void loadUser_deletesExpiredAccounts() {
            var expired = new User();
            expired.setEmail("old@example.com");
            when(userRepository.findByMarkedDeletionBefore(any())).thenReturn(List.of(expired));
            when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

            service.loadUserByUsername("test@example.com");

            verify(userRepository).delete(expired);
        }

        @Test
        @DisplayName("Login cancels pending deletion")
        void loadUser_cancelsPendingDeletion() {
            user.setMarkedDeletion(LocalDateTime.now().plusHours(1));
            when(userRepository.findByMarkedDeletionBefore(any())).thenReturn(List.of());
            when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

            service.loadUserByUsername("test@example.com");

            assertThat(user.getMarkedDeletion()).isNull();
            verify(userRepository).save(user);
        }
    }

    // Profile update flow.
    @Nested
    @DisplayName("Profile Update")
    class ProfileUpdate {

        @BeforeEach
        void setId() {
            user = mock(User.class);
            when(user.getEmail()).thenReturn("test@example.com");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("Name is updated correctly")
        void updateProfile_setsNameAndSaves() {
            service.updateProfile(1L, "Anna", "Schmidt");

            verify(user).setFirstName("Anna");
            verify(user).setLastName("Schmidt");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Unknown ID throws IllegalArgumentException")
        void updateProfile_unknownId_throws() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateProfile(99L, "X", "Y"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }
    }

    // Account deletion flow.
    @Nested
    @DisplayName("Account Deletion")
    class AccountDeletion {

        @BeforeEach
        void stub() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        }

        @Test
        @DisplayName("Deletion request sets MarkedDeletion to ~2 hours in the future")
        void requestDeletion_setsMarkedDeletion() {
            var before = LocalDateTime.now().plusHours(2).minusSeconds(5);
            var after  = LocalDateTime.now().plusHours(2).plusSeconds(5);

            service.requestAccountDeletion(1L);

            assertThat(user.getMarkedDeletion()).isBetween(before, after);
        }

        @Test
        @DisplayName("Deletion request saves the user")
        void requestDeletion_savesUser() {
            service.requestAccountDeletion(1L);

            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Deletion request logs a SYSTEM activity")
        void requestDeletion_logsActivity() {
            service.requestAccountDeletion(1L);

            verify(activityService).logActivity(eq(user), eq("SYSTEM"), any(), any());
        }
    }

    // Settings flow.
    @Nested
    @DisplayName("User Settings")
    class Settings {

        @Test
        @DisplayName("getUserSettings returns existing settings")
        void getSettings_existingSettings_returned() {
            var existing = new UserSettings(user);
            when(userSettingsRepository.findByUser(user)).thenReturn(Optional.of(existing));

            var result = service.getUserSettings(user);

            assertThat(result).isSameAs(existing);
            verify(userSettingsRepository, never()).save(any());
        }

        @Test
        @DisplayName("getUserSettings creates and saves default settings if none exist")
        void getSettings_noSettings_createsDefault() {
            when(userSettingsRepository.findByUser(user)).thenReturn(Optional.empty());

            service.getUserSettings(user);

            verify(userSettingsRepository).save(any(UserSettings.class));
        }

        @Test
        @DisplayName("updateUserSettings updates darkMode")
        void updateSettings_darkMode() {
            var existing = new UserSettings(user);
            when(userSettingsRepository.findByUser(user)).thenReturn(Optional.of(existing));

            service.updateUserSettings(user, new UserSettingsDto(true, null, null));

            assertThat(existing.isDarkMode()).isTrue();
        }

        @Test
        @DisplayName("updateUserSettings updates priceAlerts")
        void updateSettings_priceAlerts() {
            var existing = new UserSettings(user);
            when(userSettingsRepository.findByUser(user)).thenReturn(Optional.of(existing));

            service.updateUserSettings(user, new UserSettingsDto(null, false, null));

            assertThat(existing.isPriceAlerts()).isFalse();
        }

        @Test
        @DisplayName("updateUserSettings updates transactionUpdates")
        void updateSettings_transactionUpdates() {
            var existing = new UserSettings(user);
            when(userSettingsRepository.findByUser(user)).thenReturn(Optional.of(existing));

            service.updateUserSettings(user, new UserSettingsDto(null, null, false));

            assertThat(existing.isTransactionUpdates()).isFalse();
        }

        @Test
        @DisplayName("Null fields in DTO are ignored (no overwrite)")
        void updateSettings_nullFieldsIgnored() {
            var existing = new UserSettings(user);
            existing.setDarkMode(true);
            existing.setPriceAlerts(true);
            when(userSettingsRepository.findByUser(user)).thenReturn(Optional.of(existing));

            service.updateUserSettings(user, new UserSettingsDto(null, null, null));

            assertThat(existing.isDarkMode()).isTrue();
            assertThat(existing.isPriceAlerts()).isTrue();
        }
    }
}
