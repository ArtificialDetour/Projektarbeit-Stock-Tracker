package com.project.stocktracker.repository;

import com.project.stocktracker.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should find user by email ignoring case")
    void findByEmailIgnoreCase() {
        User user = User.create("Test@Example.com", "pass123", "John", "Doe");
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmailIgnoreCase("test@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should find users marked for deletion before a certain time")
    void findByMarkedDeletionBefore() {
        User user = User.create("test@example.com", "pass123", "John", "Doe");
        // Simulate an expired deletion marker.
        user.setMarkedDeletion(LocalDateTime.now().minusHours(2));
        userRepository.save(user);

        User safeUser = User.create("safe@example.com", "pass123", "Jane", "Doe");
        userRepository.save(safeUser);

        List<User> toDelete = userRepository.findByMarkedDeletionBefore(LocalDateTime.now().minusHours(1));
        
        assertThat(toDelete).hasSize(1);
        assertThat(toDelete.get(0).getEmail()).isEqualTo("test@example.com");
    }
}
