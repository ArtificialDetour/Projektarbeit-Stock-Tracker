package com.project.stocktracker.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileUpdateRequestDtoTest {

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
        var dto = new UserProfileUpdateRequestDto("John", "Doe");
        Set<ConstraintViolation<UserProfileUpdateRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when firstName is blank")
    void blankFirstName_failsValidation() {
        var dto = new UserProfileUpdateRequestDto("", "Doe");
        Set<ConstraintViolation<UserProfileUpdateRequestDto>> violations = validator.validate(dto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("First name is required");
    }

    @Test
    @DisplayName("Should fail validation when lastName is blank")
    void blankLastName_failsValidation() {
        var dto = new UserProfileUpdateRequestDto("John", "");
        Set<ConstraintViolation<UserProfileUpdateRequestDto>> violations = validator.validate(dto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Last name is required");
    }

    @Test
    @DisplayName("Should fail validation when firstName exceeds max length")
    void longFirstName_failsValidation() {
        String longName = "A".repeat(101);
        var dto = new UserProfileUpdateRequestDto(longName, "Doe");
        Set<ConstraintViolation<UserProfileUpdateRequestDto>> violations = validator.validate(dto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("First name must be at most 100 characters");
    }
}
