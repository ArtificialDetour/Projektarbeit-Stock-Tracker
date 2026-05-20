package com.project.stocktracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stocktracker.dto.RegistrationResultDto;
import com.project.stocktracker.dto.UserProfileUpdateRequestDto;
import com.project.stocktracker.dto.UserSettingsDto;
import com.project.stocktracker.entity.User;
import com.project.stocktracker.entity.UserSettings;
import com.project.stocktracker.service.ActivityService;
import com.project.stocktracker.service.NotificationService;
import com.project.stocktracker.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private ActivityService activityService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AuthController controller;

    private MockMvc mockMvc;
    private User mockUser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@example.com");
        mockUser.setFirstName("John");
        mockUser.setLastName("Doe");

        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UsernamePasswordAuthenticationToken auth;

    private void authenticate() {
        auth = new UsernamePasswordAuthenticationToken(mockUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void loginPage_returnsLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void loginPage_authenticated_redirectsToDashboard() throws Exception {
        authenticate();
        mockMvc.perform(get("/login").principal(auth))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void registerPage_returnsRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void register_success() throws Exception {
        when(userService.registerUser(any())).thenReturn(new RegistrationResultDto(mockUser, "ABCD2345"));

        mockMvc.perform(post("/register")
                        .param("email", "new@example.com")
                        .param("password", "Pass123!")
                        .param("passwordConfirm", "Pass123!")
                        .param("firstName", "John")
                        .param("lastName", "Doe"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("registrationComplete", true))
                .andExpect(model().attribute("securityCode", "ABCD2345"))
                .andExpect(model().attribute("registeredEmail", "test@example.com"));
    }

    @Test
    void register_passwordsDoNotMatch_returnsRegisterView() throws Exception {
        mockMvc.perform(post("/register")
                        .param("email", "new@example.com")
                        .param("password", "Pass123!")
                        .param("passwordConfirm", "WrongPass123!")
                        .param("firstName", "John")
                        .param("lastName", "Doe"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().hasErrors());
    }

    @Test
    void forgotPassword_success() throws Exception {
        mockMvc.perform(post("/forgot-password")
                        .param("email", "test@example.com")
                        .param("securityCode", "ABCD2345")
                        .param("newPassword", "NewPass123!")
                        .param("confirmPassword", "NewPass123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?reset=true"));

        verify(userService).resetPassword("test@example.com", "ABCD2345", "NewPass123!");
    }

    @Test
    void forgotPassword_mismatch_returnsLoginView() throws Exception {
        mockMvc.perform(post("/forgot-password")
                        .param("email", "test@example.com")
                        .param("securityCode", "ABCD2345")
                        .param("newPassword", "NewPass123!")
                        .param("confirmPassword", "WrongPass123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("showForgotPasswordForm", true));
    }

    @Test
    void forgotPassword_serviceFailure_returnsNeutralError() throws Exception {
        doThrow(new IllegalArgumentException("Password reset failed"))
                .when(userService).resetPassword("test@example.com", "WRONG999", "NewPass123!");

        mockMvc.perform(post("/forgot-password")
                        .param("email", "test@example.com")
                        .param("securityCode", "WRONG999")
                        .param("newPassword", "NewPass123!")
                        .param("confirmPassword", "NewPass123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("showForgotPasswordForm", true))
                .andExpect(model().attribute("errorMessage",
                        "Password reset failed. Check your email, security code, and password confirmation."));
    }

    @Test
    void currentUser_authenticated_returnsProfile() throws Exception {
        authenticate();
        when(userService.getUserSettings(mockUser)).thenReturn(new UserSettings());

        mockMvc.perform(get("/api/auth/me").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.displayName").value("John Doe"));
    }

    @Test
    void updateProfile_success() throws Exception {
        authenticate();
        when(userService.updateProfile(anyLong(), anyString(), anyString())).thenReturn(mockUser);
        when(userService.getUserSettings(mockUser)).thenReturn(new UserSettings());

        UserProfileUpdateRequestDto req = new UserProfileUpdateRequestDto("Jane", "Doe");

        mockMvc.perform(patch("/api/auth/profile").principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));

        verify(activityService).logActivity(any(), eq("SETTINGS"), any(), any());
    }

    @Test
    void requestAccountDeletion_success() throws Exception {
        authenticate();

        mockMvc.perform(post("/api/auth/delete-account").principal(auth))
                .andExpect(status().isOk());

        verify(userService).requestAccountDeletion(1L);
        verify(notificationService).createNotification(any(), any(), any(), any(), any());
    }

    @Test
    void updateSettings_success() throws Exception {
        authenticate();
        when(userService.getUserById(anyLong())).thenReturn(mockUser);
        when(userService.getUserSettings(mockUser)).thenReturn(new UserSettings());

        UserSettingsDto req = new UserSettingsDto(true, true, true);

        mockMvc.perform(patch("/api/auth/settings").principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(userService).updateUserSettings(eq(mockUser), any());
        verify(activityService, never()).logActivity(any(), eq("SETTINGS"), any(), any());
    }
}
