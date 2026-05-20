package com.project.stocktracker.repository;

import com.project.stocktracker.entity.Transaction;
import com.project.stocktracker.entity.TransactionStatus;
import com.project.stocktracker.entity.TransactionType;
import com.project.stocktracker.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.create("test@example.com", "pass123", "John", "Doe");
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should find transactions by user ordered by created date asc")
    void findByUserOrderByCreatedAtAsc() throws InterruptedException {
        Transaction tx1 = new Transaction();
        tx1.setUser(testUser);
        tx1.setSymbol("AAPL");
        tx1.setAssetName("Apple");
        tx1.setQuantity(BigDecimal.ONE);
        tx1.setPricePerShare(BigDecimal.ONE);
        tx1.setTotalAmount(BigDecimal.ONE);
        tx1.setTransactionType(TransactionType.BUY);
        tx1.setStatus(TransactionStatus.SETTLED);
        transactionRepository.save(tx1);

        // Ensure a stable ordering timestamp.
        Thread.sleep(10);

        Transaction tx2 = new Transaction();
        tx2.setUser(testUser);
        tx2.setSymbol("MSFT");
        tx2.setAssetName("Microsoft");
        tx2.setQuantity(BigDecimal.ONE);
        tx2.setPricePerShare(BigDecimal.ONE);
        tx2.setTotalAmount(BigDecimal.ONE);
        tx2.setTransactionType(TransactionType.BUY);
        tx2.setStatus(TransactionStatus.SETTLED);
        transactionRepository.save(tx2);

        List<Transaction> transactions = transactionRepository.findByUserOrderByCreatedAtAsc(testUser);
        
        assertThat(transactions).hasSize(2);
        assertThat(transactions.get(0).getSymbol()).isEqualTo("AAPL");
        assertThat(transactions.get(1).getSymbol()).isEqualTo("MSFT");
    }
}
