package com.project.stocktracker.repository;

import com.project.stocktracker.entity.Activity;
import com.project.stocktracker.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ActivityRepositoryTest {

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.create("test@example.com", "pass123", "John", "Doe");
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should find activities by user ordered by created date descending")
    void findByUserOrderByCreatedAtDesc() {
        Activity act1 = new Activity();
        act1.setUser(testUser);
        act1.setTitle("First");
        act1.setDescription("Desc");
        act1.setType("LOGIN");
        activityRepository.save(act1);

        Activity act2 = new Activity();
        act2.setUser(testUser);
        act2.setTitle("Second");
        act2.setDescription("Desc");
        act2.setType("LOGIN");
        activityRepository.save(act2);

        List<Activity> activities = activityRepository.findByUserOrderByCreatedAtDesc(testUser);
        
        assertThat(activities).hasSize(2);
        assertThat(activities.get(0).getTitle()).isEqualTo("Second");
    }

    @Test
    @DisplayName("Should delete all activities by user via custom query")
    void deleteByUser() {
        Activity act = new Activity();
        act.setUser(testUser);
        act.setTitle("First");
        act.setDescription("Desc");
        act.setType("LOGIN");
        activityRepository.save(act);

        assertThat(activityRepository.findByUserOrderByCreatedAtDesc(testUser)).hasSize(1);

        activityRepository.deleteByUser(testUser);

        assertThat(activityRepository.findByUserOrderByCreatedAtDesc(testUser)).isEmpty();
    }
}
