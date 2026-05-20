package com.project.stocktracker.repository;

import com.project.stocktracker.entity.Transaction;
import com.project.stocktracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for persisted buy and sell transactions.
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    /**
     * Finds all user transactions, newest first.
     */
    List<Transaction> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Finds all user transactions, oldest first.
     */
    List<Transaction> findByUserOrderByCreatedAtAsc(User user);

    /**
     * Finds all transactions for one symbol, oldest first.
     */
    List<Transaction> findByUserAndSymbolOrderByCreatedAtAsc(User user, String symbol);

    /**
     * Deletes all transactions owned by a user.
     */
    void deleteByUser(User user);
}
