package com.project.stocktracker.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Current open position for one user and asset symbol.
 */
@Entity
@Table(name = "holdings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "symbol"}))
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(name = "asset_name", nullable = false)
    private String assetName;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;

    @Column(name = "avg_cost_basis", nullable = false, precision = 18, scale = 6)
    private BigDecimal avgCostBasis;

    @Column(name = "first_purchase_date", nullable = false)
    private LocalDateTime firstPurchaseDate;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    protected void onCreate() {
        if (firstPurchaseDate == null) firstPurchaseDate = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getAssetName() { return assetName; }
    public void setAssetName(String assetName) { this.assetName = assetName; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getAvgCostBasis() { return avgCostBasis; }
    public void setAvgCostBasis(BigDecimal avgCostBasis) { this.avgCostBasis = avgCostBasis; }

    public LocalDateTime getFirstPurchaseDate() { return firstPurchaseDate; }
    public void setFirstPurchaseDate(LocalDateTime firstPurchaseDate) { this.firstPurchaseDate = firstPurchaseDate; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
