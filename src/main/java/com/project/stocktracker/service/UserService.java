package com.project.stocktracker.service;

import com.project.stocktracker.dto.RegistrationResultDto;
import com.project.stocktracker.dto.UserRegistrationDto;
import com.project.stocktracker.dto.UserSettingsDto;
import com.project.stocktracker.entity.User;
import com.project.stocktracker.entity.UserSettings;
import com.project.stocktracker.repository.HoldingRepository;
import com.project.stocktracker.repository.NotificationRepository;
import com.project.stocktracker.repository.TransactionRepository;
import com.project.stocktracker.repository.UserRepository;
import com.project.stocktracker.repository.UserSettingsRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manages users, authentication lookup, settings, and account lifecycle workflows.
 */
@Service
public class UserService implements UserDetailsService {

    private static final Logger log = Logger.getLogger(UserService.class.getName());
    private static final String SECURITY_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int SECURITY_CODE_LENGTH = 8;

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityService activityService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserService(UserRepository userRepository, UserSettingsRepository userSettingsRepository,
                       HoldingRepository holdingRepository, TransactionRepository transactionRepository,
                       NotificationRepository notificationRepository, ActivityService activityService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.holdingRepository = holdingRepository;
        this.transactionRepository = transactionRepository;
        this.notificationRepository = notificationRepository;
        this.activityService = activityService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Loads a user for Spring Security and also cancels pending deletion when the user signs in again.
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Deletion cleanup currently runs opportunistically before authentication.
        LocalDateTime now = LocalDateTime.now();
        List<User> expiredUsers = userRepository.findByMarkedDeletionBefore(now);
        for (User expiredUser : expiredUsers) {
            log.info("Deleting expired account: " + expiredUser.getEmail());
            userSettingsRepository.deleteByUser(expiredUser);
            holdingRepository.deleteByUser(expiredUser);
            transactionRepository.deleteByUser(expiredUser);
            notificationRepository.deleteByUser(expiredUser);
            activityService.clearActivities(expiredUser);
            userRepository.delete(expiredUser);
        }

        String normalizedEmail = normalizeEmail(email);
        log.fine("Loading user details for email: " + normalizedEmail);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + normalizedEmail));

        // Logging in during the grace period is treated as an explicit deletion cancellation.
        if (user.getMarkedDeletion() != null) {
            log.info("Cancelling account deletion for: " + normalizedEmail);
            user.setMarkedDeletion(null);
            user = userRepository.save(user);
        }

        return user;
    }

    /**
     * Registers a user with a normalized unique email and creates default settings.
     */
    @Transactional
    public RegistrationResultDto registerUser(UserRegistrationDto dto) {
        String normalizedEmail = normalizeEmail(dto.email());
        log.info("Registering new user with email: " + normalizedEmail);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email already registered");
        }

        String securityCode = generateSecurityCode();
        User user = User.create(
            normalizedEmail,
                passwordEncoder.encode(dto.password()),
                dto.firstName(),
                dto.lastName()
        );
        user.setSecurityCodeHash(passwordEncoder.encode(securityCode));

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with id: " + savedUser.getId());

        UserSettings defaultSettings = new UserSettings(savedUser);
        userSettingsRepository.save(defaultSettings);

        return new RegistrationResultDto(savedUser, securityCode);
    }

    /**
     * Finds a user by normalized email for account-level workflows.
     */
    public User findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + normalizedEmail));
    }

    /**
     * Returns all users for application-wide background workflows.
     */
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase();
    }

    private String normalizeSecurityCode(String securityCode) {
        if (securityCode == null) {
            return "";
        }
        return securityCode.trim().toUpperCase();
    }

    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    private String generateSecurityCode() {
        StringBuilder code = new StringBuilder(SECURITY_CODE_LENGTH);
        for (int i = 0; i < SECURITY_CODE_LENGTH; i++) {
            int index = secureRandom.nextInt(SECURITY_CODE_ALPHABET.length());
            code.append(SECURITY_CODE_ALPHABET.charAt(index));
        }
        return code.toString();
    }

    /**
     * Updates profile fields owned by the settings page.
     */
    @Transactional
    public User updateProfile(Long userId, String firstName, String lastName) {
        log.info("Updating profile for user id: " + userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setFirstName(firstName);
        user.setLastName(lastName);

        return userRepository.save(user);
    }

    /**
     * Resolves a user by id when the caller already trusts the identity source.
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }

    /**
     * Marks an account for delayed deletion so the user can cancel by signing in again.
     */
    @Transactional
    public void requestAccountDeletion(Long userId) {
        User user = getUserById(userId);
        user.setMarkedDeletion(LocalDateTime.now().plusHours(2));
        userRepository.save(user);
        log.info("Account deletion requested for user id: " + userId + ". Will be deleted after 2 hours.");
        activityService.logActivity(user, "SYSTEM", "Account Deletion Requested", "You started the account deletion process. Your data will be removed in 2 hours.");
    }

    /**
     * Returns persisted settings or creates defaults for older users without a settings row.
     */
    public UserSettings getUserSettings(User user) {
        return userSettingsRepository.findByUser(user)
                .orElseGet(() -> {
                    UserSettings defaultSettings = new UserSettings(user);
                    return userSettingsRepository.save(defaultSettings);
                });
    }

    /**
     * Applies partial settings updates; null fields intentionally leave existing values unchanged.
     */
    @Transactional
    public UserSettings updateUserSettings(User user, UserSettingsDto dto) {
        UserSettings settings = getUserSettings(user);

        if (dto.darkMode() != null) {
            settings.setDarkMode(dto.darkMode());
        }
        if (dto.priceAlerts() != null) {
            settings.setPriceAlerts(dto.priceAlerts());
        }
        if (dto.transactionUpdates() != null) {
            settings.setTransactionUpdates(dto.transactionUpdates());
        }

        return userSettingsRepository.save(settings);
    }

    /**
     * Replaces the stored password hash after validating the user's recovery code.
     */
    @Transactional
    public void resetPassword(String email, String securityCode, String newPassword) {
        if (!isValidPassword(newPassword)) {
            throw new IllegalArgumentException("Password reset failed");
        }

        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
            .orElseThrow(() -> new IllegalArgumentException("Password reset failed"));

        String normalizedSecurityCode = normalizeSecurityCode(securityCode);
        if (normalizedSecurityCode.isEmpty()
                || user.getSecurityCodeHash() == null
                || !passwordEncoder.matches(normalizedSecurityCode, user.getSecurityCodeHash())) {
            throw new IllegalArgumentException("Password reset failed");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password reset successfully for user: " + normalizedEmail);
    }
}
