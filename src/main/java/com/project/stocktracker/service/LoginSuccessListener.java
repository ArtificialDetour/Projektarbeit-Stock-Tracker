package com.project.stocktracker.service;

import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Receives successful authentication events.
 */
@Component
public class LoginSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {

    private final ActivityService activityService;

    public LoginSuccessListener(ActivityService activityService) {
        this.activityService = activityService;
    }

    /**
     * Handles successful login events.
     */
    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        // Logins are no longer logged to the activity list as per user request.
    }
}
