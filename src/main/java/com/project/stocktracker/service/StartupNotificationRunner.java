package com.project.stocktracker.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

/**
 * Generates startup notifications after the application context is ready.
 */
@Component
public class StartupNotificationRunner implements CommandLineRunner {

    private static final Logger log = Logger.getLogger(StartupNotificationRunner.class.getName());

    private final UserService userService;
    private final NotificationService notificationService;

    public StartupNotificationRunner(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    /**
     * Runs the startup notification workflow.
     */
    @Override
    public void run(String... args) {
        log.info("App restarted - generating performance notifications for all users...");
        try {
            userService.findAllUsers().forEach(user -> {
                try {
                    notificationService.sendStartupPerformanceNotifications(user);
                } catch (Exception e) {
                    log.warning("Failed to generate notifications for user " + user.getEmail() + ": " + e.getMessage());
                }
            });
            log.info("Startup notifications generated successfully.");
        } catch (Exception e) {
            log.severe("Critical failure in startup notification runner: " + e.getMessage());
        }
    }
}
