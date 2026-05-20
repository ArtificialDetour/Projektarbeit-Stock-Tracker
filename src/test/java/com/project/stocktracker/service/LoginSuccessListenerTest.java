package com.project.stocktracker.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LoginSuccessListenerTest {

    @Mock
    private ActivityService activityService;

    @InjectMocks
    private LoginSuccessListener listener;

    @Test
    @DisplayName("Should not log activity on login success")
    void onApplicationEvent_doesNotLogActivity() {
        Authentication auth = mock(Authentication.class);
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(auth);

        listener.onApplicationEvent(event);

        verifyNoInteractions(activityService);
    }
}
