package com.project.stocktracker.repository;

import com.project.stocktracker.entity.Holding;
import com.project.stocktracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for current portfolio holdings.
 */
public interface HoldingRepository extends JpaRepository<Holding, Long> {
    /**
     * Finds one open holding by user and ticker symbol.
     */
    Optional<Holding> findByUserAndSymbol(User user, String symbol);

    /**
     * Finds all open holdings for a user.
     */
    List<Holding> findByUser(User user);

    /**
     * Deletes all holdings owned by a user.
     */
    void deleteByUser(User user);
}
