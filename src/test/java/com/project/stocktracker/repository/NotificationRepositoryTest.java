package com.project.stocktracker.repository;

import com.project.stocktracker.entity.Notification;
import com.project.stocktracker.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.create("test@example.com", "pass123", "John", "Doe");
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should count unread notifications")
    void countByUserAndReadFalse() {
        Notification n1 = new Notification();
        n1.setUser(testUser);
        n1.setTitle("N1");
        n1.setMessage("Msg1");
        n1.setRead(false);
        notificationRepository.save(n1);

        Notification n2 = new Notification();
        n2.setUser(testUser);
        n2.setTitle("N2");
        n2.setMessage("Msg2");
        n2.setRead(true);
        notificationRepository.save(n2);

        long unreadCount = notificationRepository.countByUserAndReadFalse(testUser);
        assertThat(unreadCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should mark all notifications as read using Modifying query")
    void markAllReadByUser() {
        Notification n1 = new Notification();
        n1.setUser(testUser);
        n1.setTitle("N1");
        n1.setMessage("Msg1");
        n1.setRead(false);
        notificationRepository.save(n1);

        notificationRepository.markAllReadByUser(testUser);
        entityManager.flush();
        entityManager.clear();

        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(testUser);
        assertThat(notifications.get(0).isRead()).isTrue();
    }
}
