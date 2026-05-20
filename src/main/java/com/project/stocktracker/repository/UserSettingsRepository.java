package com.project.stocktracker.repository;

import com.project.stocktracker.entity.User;
import com.project.stocktracker.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for per-user preference records.
 */
@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
    /**
     * Finds the settings row for a user.
     */
    Optional<UserSettings> findByUser(User user);

    /**
     * Deletes the settings row for a user.
     */
    void deleteByUser(User user);
}
