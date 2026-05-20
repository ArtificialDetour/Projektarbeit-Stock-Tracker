package com.project.stocktracker.controller;

import com.project.stocktracker.dto.RegistrationResultDto;
import com.project.stocktracker.dto.UserProfileResponseDto;
import com.project.stocktracker.dto.UserProfileUpdateRequestDto;
import com.project.stocktracker.dto.UserRegistrationDto;
import com.project.stocktracker.dto.UserSettingsDto;
import com.project.stocktracker.entity.User;
import com.project.stocktracker.entity.UserSettings;
import com.project.stocktracker.service.NotificationService;
import com.project.stocktracker.service.UserService;
import com.project.stocktracker.service.ActivityService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.logging.Logger;

/**
 * Handles authentication pages and authenticated account endpoints.
 */
@Controller
public class AuthController {

    private static final Logger log = Logger.getLogger(AuthController.class.getName());

    private final UserService userService;
    private final ActivityService activityService;
    private final NotificationService notificationService;

    public AuthController(UserService userService, ActivityService activityService, NotificationService notificationService) {
        this.userService = userService;
        this.activityService = activityService;
        this.notificationService = notificationService;
    }

    /**
     * Displays the login page and maps redirect query flags to user-facing messages.
     */
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "registered", required = false) String registered,
            @RequestParam(value = "session", required = false) String session,
            @RequestParam(value = "deleted", required = false) String deleted,
            HttpServletRequest request,
            Authentication authentication,
            Model model) {

        log.fine("GET /login");

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }

        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "Successfully logged out");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Registration successful. Please login.");
        }
        if (session != null) {
            model.addAttribute("errorMessage", "Session expired. Please login again");
        }
        if (deleted != null) {
            model.addAttribute("successMessage", "Account deletion requested. Your account will be permanently deleted in 2 hours. Log back in to cancel.");
        }
        String reset = request.getParameter("reset");
        if (reset != null) {
            model.addAttribute("successMessage", "Password has been reset successfully. Please sign in with your new password.");
        }

        return "login";
    }

    /**
     * Displays an empty registration form.
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        log.fine("GET /register");
        model.addAttribute("user", new UserRegistrationDto(null, null, null, null, null));
        return "register";
    }

    /**
     * Creates a new account after DTO validation and password confirmation.
     */
    @PostMapping("/register")
    public String register(
            @Valid UserRegistrationDto dto,
            BindingResult bindingResult,
            Model model) {

        log.fine("POST /register - email: " + dto.email());

        // Cross-field validation lives here because the DTO exposes only the comparison helper.
        if (!dto.isPasswordMatch()) {
            bindingResult.rejectValue("passwordConfirm", "error.passwordConfirm", "Passwords do not match");
        }

        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            RegistrationResultDto result = userService.registerUser(dto);
            log.info("User registered successfully: " + result.user().getEmail());
            model.addAttribute("user", dto);
            model.addAttribute("registrationComplete", true);
            model.addAttribute("securityCode", result.securityCode());
            model.addAttribute("registeredEmail", result.user().getEmail());
            return "register";
        } catch (IllegalArgumentException e) {
            log.warning("Registration failed: " + e.getMessage());
            bindingResult.rejectValue("email", "error.email", e.getMessage());
            return "register";
        }
    }

    /**
     * Handles the current password reset form while preserving account-enumeration protection.
     */
    @PostMapping("/forgot-password")
    public String forgotPassword(
            @RequestParam("email") String email,
            @RequestParam("securityCode") String securityCode,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {
        log.fine("POST /forgot-password - email: " + email);

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Passwords do not match");
            model.addAttribute("showForgotPasswordForm", true);
            return "login";
        }
        
        try {
            userService.resetPassword(email, securityCode, newPassword);
            return "redirect:/login?reset=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", "Password reset failed. Check your email, security code, and password confirmation.");
            model.addAttribute("showForgotPasswordForm", true);
            return "login";
        }
    }

    /**
     * Returns profile and settings data consumed by the shared top navigation and settings page.
     */
    @GetMapping("/api/auth/me")
    @ResponseBody
    public ResponseEntity<UserProfileResponseDto> currentUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(toProfileResponse(user));
    }

    /**
     * Updates the editable profile fields for the authenticated user.
     */
    @PatchMapping("/api/auth/profile")
    @ResponseBody
    public ResponseEntity<UserProfileResponseDto> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UserProfileUpdateRequestDto request) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User principalUser = (User) authentication.getPrincipal();
        User updatedUser = userService.updateProfile(
                principalUser.getId(),
                request.firstName().trim(),
                request.lastName().trim()
        );

        activityService.logActivity(updatedUser, "SETTINGS", "Profile Updated", "Your profile information was updated successfully.");

        return ResponseEntity.ok(toProfileResponse(updatedUser));
    }

    /**
     * Starts delayed account deletion and invalidates the current session immediately.
     */
    @PostMapping("/api/auth/delete-account")
    @ResponseBody
    public ResponseEntity<Void> requestAccountDeletion(Authentication authentication, HttpServletRequest request) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User principalUser = (User) authentication.getPrincipal();
        userService.requestAccountDeletion(principalUser.getId());

        notificationService.createNotification(principalUser,
                "Account Deletion Pending",
                "Your account will be permanently deleted in 2 hours. Log back in to cancel.",
                "warning", "tertiary");
        
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok().build();
    }

    /**
     * Applies partial settings updates from toggle controls.
     */
    @PatchMapping("/api/auth/settings")
    @ResponseBody
    public ResponseEntity<UserProfileResponseDto> updateSettings(
            Authentication authentication,
            @RequestBody UserSettingsDto request) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User principalUser = (User) authentication.getPrincipal();
        userService.updateUserSettings(principalUser, request);

        // Reload the user so the response reflects settings persisted in the current transaction.
        User updatedUser = userService.getUserById(principalUser.getId());
        return ResponseEntity.ok(toProfileResponse(updatedUser));
    }

    private UserProfileResponseDto toProfileResponse(User user) {
        UserSettings settings = userService.getUserSettings(user);

        return new UserProfileResponseDto(
                user.getEmail(),
                user.getFirstName() == null ? "" : user.getFirstName().trim(),
                user.getLastName() == null ? "" : user.getLastName().trim(),
                user.getFullName(),
                settings.isDarkMode(),
                settings.isPriceAlerts(),
                settings.isTransactionUpdates()
        );
    }

    /**
     * Returns the current application user from the Spring Security context, if present.
     */
    public static User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return (User) auth.getPrincipal();
        }
        return null;
    }

    /**
     * Checks whether the current security context contains an application user.
     */
    public static boolean isAuthenticated() {
        return getCurrentUser() != null;
    }
}
