package com.project.stocktracker.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("Should return full name when both first and last name are present")
    void getFullName_bothNamesPresent() {
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");

        assertThat(user.getFullName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should return only first name when last name is null or empty")
    void getFullName_onlyFirstNamePresent() {
        User user = new User();
        user.setFirstName("John");
        user.setLastName("");

        assertThat(user.getFullName()).isEqualTo("John");

        user.setLastName(null);
        assertThat(user.getFullName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should return email when first name is null or empty")
    void getFullName_noFirstName_returnsEmail() {
        User user = new User();
        user.setEmail("test@example.com");
        
        user.setFirstName("");
        assertThat(user.getFullName()).isEqualTo("test@example.com");

        user.setFirstName(null);
        assertThat(user.getFullName()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("create() should initialize user correctly")
    void create_initializesUser() {
        User user = User.create("test@example.com", "pass123", "Jane", "Doe");

        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPassword()).isEqualTo("pass123");
        assertThat(user.getFirstName()).isEqualTo("Jane");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("Security code hash can be stored and read")
    void securityCodeHash_getterSetter() {
        User user = new User();

        user.setSecurityCodeHash("encoded_ABCD2345");

        assertThat(user.getSecurityCodeHash()).isEqualTo("encoded_ABCD2345");
    }

    @Test
    @DisplayName("UserDetails methods should return default true/ROLE_USER")
    void userDetailsMethods() {
        User user = new User();
        user.setEmail("user@test.com");

        assertThat(user.getUsername()).isEqualTo("user@test.com");
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
        assertThat(user.isEnabled()).isTrue();

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }
}
