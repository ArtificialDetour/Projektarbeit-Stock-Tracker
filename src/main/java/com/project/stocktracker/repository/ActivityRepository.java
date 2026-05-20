package com.project.stocktracker.repository;

import com.project.stocktracker.entity.Activity;
import com.project.stocktracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

/**
 * Repository for querying and pruning user activity feed entries.
 */
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    /**
     * Finds all activity entries for a user, newest first.
     */
    @Query("SELECT a FROM Activity a WHERE a.user = :user ORDER BY a.createdAt DESC, a.id DESC")
    List<Activity> findByUserOrderByCreatedAtDesc(@Param("user") User user);

    /**
     * Finds the latest twenty activity entries for a user.
     */
    List<Activity> findTop20ByUserOrderByCreatedAtDescIdDesc(User user);
    
    /**
     * Deletes all activity entries owned by a user.
     */
    @Modifying
    @Query("DELETE FROM Activity a WHERE a.user = :user")
    void deleteByUser(@Param("user") User user);
}
