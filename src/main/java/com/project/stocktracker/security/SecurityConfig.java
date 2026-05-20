package com.project.stocktracker.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures authentication, authorization, CSRF, and password hashing.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Uses BCrypt for one-way password hashing.
     * A strength of 12 keeps login latency acceptable while making offline attacks more expensive.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Defines the web security rules for public pages, static assets, form login and logout.
     * Matcher order matters: specific authenticated API endpoints must be listed before broader public paths.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // Profile endpoints expose user-specific data and must always require a logged-in user.
                        .requestMatchers("/api/auth/me", "/api/auth/profile").authenticated()
                        // Only unauthenticated entry points and browser metadata are publicly reachable.
                        .requestMatchers("/", "/login", "/forgot-password", "/register", "/favicon.ico").permitAll()
                        // Static resources are served without authentication so public and protected pages can render.
                        .requestMatchers(
                                "/static/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/icons/**",
                                "/fonts/**",
                                "/scripts/**",
                                "/styles/**",
                                "/components/**"
                        ).permitAll()
                        // Default-deny for application routes: anything not listed above requires authentication.
                        .anyRequest().authenticated()
                )
                .formLogin(formLogin -> formLogin
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .deleteCookies("JSESSIONID")
                        .clearAuthentication(true)
                        .permitAll()
                )
                .csrf(csrf -> {
                    // Keep Spring Security's default CSRF protection enabled.
                });

        return http.build();
    }
}
