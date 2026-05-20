package com.project.stocktracker.entity;

import jakarta.persistence.*;

/**
 * User-owned feature and display preferences.
 */
@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "dark_mode", nullable = false)
    private boolean darkMode = false;

    @Column(name = "price_alerts", nullable = false)
    private boolean priceAlerts = true;

    @Column(name = "transaction_updates", nullable = false)
    private boolean transactionUpdates = true;

    public UserSettings() {
    }

    public UserSettings(User user) {
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public boolean isPriceAlerts() {
        return priceAlerts;
    }

    public void setPriceAlerts(boolean priceAlerts) {
        this.priceAlerts = priceAlerts;
    }

    public boolean isTransactionUpdates() {
        return transactionUpdates;
    }

    public void setTransactionUpdates(boolean transactionUpdates) {
        this.transactionUpdates = transactionUpdates;
    }
}
