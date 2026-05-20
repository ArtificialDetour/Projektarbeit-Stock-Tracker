package com.project.stocktracker.service;

import com.project.stocktracker.dto.HoldingDto;
import com.project.stocktracker.dto.NotificationDto;
import com.project.stocktracker.entity.Notification;
import com.project.stocktracker.entity.User;
import com.project.stocktracker.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Creates and manages user notifications.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PortfolioService portfolioService;
    private final UserService userService;

    /**
     * Creates a notification service backed by repositories and portfolio data.
     */
    public NotificationService(NotificationRepository notificationRepository, 
                               PortfolioService portfolioService,
                               UserService userService) {
        this.notificationRepository = notificationRepository;
        this.portfolioService = portfolioService;
        this.userService = userService;
    }

    /**
     * Creates portfolio performance notifications during startup for users who enabled price alerts.
     */
    public void sendStartupPerformanceNotifications(User user) {
        if (!userService.getUserSettings(user).isPriceAlerts()) {
            return;
        }

        List<HoldingDto> holdings = portfolioService.getHoldings(user);
        if (holdings.isEmpty()) return;

        // Total performance highlights compare the user's full holding lifetime.
        holdings.stream()
                .max(Comparator.comparing(HoldingDto::performancePercent))
                .ifPresent(best -> createNotification(user, "Best Performer", 
                        String.format("Your top asset is %s (%s) with a total return of %s%%.", 
                                best.assetName(), best.symbol(), best.performancePercent().toPlainString()), 
                        "trending_up", "tertiary"));

        holdings.stream()
                .min(Comparator.comparing(HoldingDto::performancePercent))
                .ifPresent(worst -> createNotification(user, "Worst Performer", 
                        String.format("Your lowest performing asset is %s (%s) with a total return of %s%%.", 
                                worst.assetName(), worst.symbol(), worst.performancePercent().toPlainString()), 
                        "trending_down", "secondary"));

        // Weekly highlights exclude zero changes to avoid duplicate low-value notifications.
        List<HoldingDto> withWeekly = holdings.stream()
                .filter(h -> h.weeklyChangePercent() != null && h.weeklyChangePercent().compareTo(BigDecimal.ZERO) != 0)
                .toList();

        if (!withWeekly.isEmpty()) {
            withWeekly.stream()
                    .max(Comparator.comparing(HoldingDto::weeklyChangePercent))
                    .ifPresent(bestW -> createNotification(user, "Weekly Highlight", 
                            String.format("%s performed best this week with %s.", 
                                    bestW.assetName(), formatMovement(bestW.weeklyChangePercent())), 
                            "local_fire_department", "primary"));

            withWeekly.stream()
                    .min(Comparator.comparing(HoldingDto::weeklyChangePercent))
                    .ifPresent(worstW -> createNotification(user, "Weekly Low", 
                            String.format("%s had the weakest week with %s.", 
                                    worstW.assetName(), formatMovement(worstW.weeklyChangePercent())), 
                            "warning", "secondary"));
        }
    }

    private String formatMovement(BigDecimal percent) {
        // Use the absolute value in text so a negative percentage never appears after "gain".
        var direction = percent.compareTo(BigDecimal.ZERO) >= 0 ? "a gain of " : "a drop of ";
        return direction + percent.abs().toPlainString() + "%";
    }

    /**
     * Persists a notification for the given user.
     */
    public void createNotification(User user, String title, String message, String icon, String iconStyle) {
        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message);
        n.setIcon(icon);
        n.setIconStyle(iconStyle);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);
    }

    /**
     * Returns all notifications for the given user.
     */
    public List<NotificationDto> getNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream().map(this::toDto).toList();
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getTitle(), n.getMessage(),
                n.getIcon(), n.getIconStyle(), n.isRead(), timeLabel(n.getCreatedAt()));
    }

    private String timeLabel(LocalDateTime t) {
        if (t == null) return "";
        long minutes = ChronoUnit.MINUTES.between(t, LocalDateTime.now());
        if (minutes < 2)  return "Just Now";
        if (minutes < 60) return minutes + "m ago";
        long hours = ChronoUnit.HOURS.between(t, LocalDateTime.now());
        if (hours < 24)   return hours + "h ago";
        return ChronoUnit.DAYS.between(t, LocalDateTime.now()) + "d ago";
    }

    /**
     * Counts unread notifications for the given user.
     */
    public long countUnread(User user) {
        return notificationRepository.countByUserAndReadFalse(user);
    }

    /**
     * Marks all notifications for the given user as read.
     */
    @Transactional
    public void markAllRead(User user) {
        notificationRepository.markAllReadByUser(user);
    }

    /**
     * Deletes all notifications for the given user.
     */
    @Transactional
    public void deleteAll(User user) {
        notificationRepository.deleteByUser(user);
    }
}
