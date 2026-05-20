package com.project.stocktracker.dto;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class UserRegistrationDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Should pass validation with valid data")
    void validData_passesValidation() {
        var dto = new UserRegistrationDto("test@example.com", "secure123", "secure123", "John", "Doe");
        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation with invalid email format")
    void invalidEmail_failsValidation() {
        var dto = new UserRegistrationDto("invalid-email", "secure123", "secure123", "John", "Doe");
        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Email must be valid"));
    }

    @Test
    @DisplayName("Should fail validation when email is blank")
    void blankEmail_failsValidation() {
        var dto = new UserRegistrationDto("", "secure123", "secure123", "John", "Doe");
        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Email is required"));
    }

    @Test
    @DisplayName("Should fail validation when password is too short")
    void shortPassword_failsValidation() {
        var dto = new UserRegistrationDto("test@example.com", "short", "short", "John", "Doe");
        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Password must be at least 8 characters"));
    }

    @Test
    @DisplayName("isPasswordMatch should return true when passwords match")
    void isPasswordMatch_returnsTrueWhenMatching() {
        var dto = new UserRegistrationDto("test@example.com", "secure123", "secure123", "John", "Doe");
        assertThat(dto.isPasswordMatch()).isTrue();
    }

    @Test
    @DisplayName("isPasswordMatch should return false when passwords do not match")
    void isPasswordMatch_returnsFalseWhenMismatch() {
        var dto = new UserRegistrationDto("test@example.com", "secure123", "different", "John", "Doe");
        assertThat(dto.isPasswordMatch()).isFalse();
    }

    @Test
    @DisplayName("isPasswordMatch should return false when password is null")
    void isPasswordMatch_returnsFalseWhenPasswordNull() {
        var dto = new UserRegistrationDto("test@example.com", null, "secure123", "John", "Doe");
        assertThat(dto.isPasswordMatch()).isFalse();
    }
}
