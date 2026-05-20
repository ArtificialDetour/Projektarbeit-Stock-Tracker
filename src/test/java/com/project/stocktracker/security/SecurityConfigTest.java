package com.project.stocktracker.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Public endpoints (/login, /register, etc.) should be accessible without authentication")
    void publicEndpoints_areAccessible() throws Exception {
        mockMvc.perform(get("/login"))
               .andExpect(status().isOk());
               
        mockMvc.perform(get("/register"))
               .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Static resources (/css, /js) should bypass security rules")
    void staticResources_areAccessible() throws Exception {
        mockMvc.perform(get("/css/non-existent-styles.css"))
               .andExpect(status().isNotFound());
               
        mockMvc.perform(get("/js/non-existent-script.js"))
               .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Protected endpoint (/dashboard) should redirect to login when unauthenticated")
    void protectedEndpoints_redirectToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser
    @DisplayName("Protected endpoint (/dashboard) should be accessible when authenticated")
    void protectedEndpoints_accessibleWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/dashboard"))
               .andExpect(authenticated());
    }

    @Test
    @DisplayName("Invalid login should fail authentication and redirect to error page")
    void invalidLogin_fails() throws Exception {
        mockMvc.perform(formLogin("/login").user("email", "wrong@example.com").password("wrongpass"))
               .andExpect(unauthenticated())
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @WithMockUser
    @DisplayName("Logout should clear authentication and redirect to logout success page")
    void logout_worksCorrectly() throws Exception {
        mockMvc.perform(logout("/logout"))
               .andExpect(unauthenticated())
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?logout=true"));
    }
}
