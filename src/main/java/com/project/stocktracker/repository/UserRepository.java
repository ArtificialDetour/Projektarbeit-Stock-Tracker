package com.project.stocktracker.repository;

import com.project.stocktracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for account lookup and deletion lifecycle queries.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by email address
     *
     * @param email the user's email
     * @return Optional containing the user if found
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Checks if a user exists with the given email
     *
     * @param email the user's email
     * @return true if user exists, false otherwise
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Finds users whose marked_deletion timestamp is before the given time
     *
     * @param time the cutoff time
     * @return List of users marked for deletion before the given time
     */
    java.util.List<User> findByMarkedDeletionBefore(java.time.LocalDateTime time);
}
