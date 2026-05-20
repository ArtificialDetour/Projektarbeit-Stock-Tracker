package com.project.stocktracker.repository;

import com.project.stocktracker.entity.Notification;
import com.project.stocktracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for notification popover data.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Finds all notifications for a user, newest first.
     */
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Counts unread notifications for a user.
     */
    long countByUserAndReadFalse(User user);

    /**
     * Checks whether the user has any notifications.
     */
    boolean existsByUser(User user);

    /**
     * Marks all notifications for a user as read.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user = :user")
    void markAllReadByUser(@Param("user") User user);

    /**
     * Deletes all notifications owned by a user.
     */
    void deleteByUser(User user);
}
