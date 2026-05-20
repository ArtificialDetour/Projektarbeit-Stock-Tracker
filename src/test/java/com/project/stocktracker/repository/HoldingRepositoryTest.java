package com.project.stocktracker.repository;

import com.project.stocktracker.entity.Holding;
import com.project.stocktracker.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class HoldingRepositoryTest {

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.create("test@example.com", "pass123", "John", "Doe");
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should find holding by user and symbol")
    void findByUserAndSymbol() {
        Holding holding = new Holding();
        holding.setUser(testUser);
        holding.setSymbol("AAPL");
        holding.setAssetName("Apple Inc");
        holding.setQuantity(new BigDecimal("10"));
        holding.setAvgCostBasis(new BigDecimal("150.0"));
        holdingRepository.save(holding);

        Optional<Holding> found = holdingRepository.findByUserAndSymbol(testUser, "AAPL");
        
        assertThat(found).isPresent();
        assertThat(found.get().getSymbol()).isEqualTo("AAPL");
    }
}
