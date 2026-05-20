package com.project.stocktracker.repository;

import com.project.stocktracker.entity.User;
import com.project.stocktracker.entity.UserSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserSettingsRepositoryTest {

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.create("test@example.com", "pass123", "John", "Doe");
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should find user settings by user")
    void findByUser() {
        UserSettings settings = new UserSettings();
        settings.setUser(testUser);
        settings.setDarkMode(true);
        settings.setPriceAlerts(false);
        userSettingsRepository.save(settings);

        Optional<UserSettings> found = userSettingsRepository.findByUser(testUser);
        
        assertThat(found).isPresent();
        assertThat(found.get().isDarkMode()).isTrue();
    }
}
