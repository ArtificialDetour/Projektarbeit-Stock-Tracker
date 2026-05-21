package com.project.stocktracker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class DatabaseConnectionTest {

    private static final String PLACEHOLDER_PASSWORD_MARKER = "PLACEHOLDER_PUT_PASSWORD_HERE";

    @Test
    @DisplayName("Should connect to the configured development database")
    void configuredDevelopmentDatabaseConnectionIsAvailable() throws Exception {
        ConfigurableEnvironment environment = loadApplicationEnvironment();

        String url = environment.getRequiredProperty("spring.datasource.url");
        String username = environment.getRequiredProperty("spring.datasource.username");
        String password = environment.getRequiredProperty("spring.datasource.password");

        assertThat(password)
                .as("spring.datasource.password must be configured before running the application")
                .isNotBlank()
                .doesNotContain(PLACEHOLDER_PASSWORD_MARKER);

        DriverManager.setLoginTimeout((int) Duration.ofSeconds(10).toSeconds());

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            assertThat(connection.isValid((int) Duration.ofSeconds(5).toSeconds()))
                    .as("Database connection should be valid for %s", url)
                    .isTrue();
        } catch (Exception exception) {
            fail("Could not connect to configured development database: " + url, exception);
        }
    }

    private ConfigurableEnvironment loadApplicationEnvironment() throws Exception {
        ConfigurableEnvironment environment = new StandardEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

        addYamlPropertySources(loader, propertySources, "application-dev.yaml");
        addYamlPropertySources(loader, propertySources, "application.yaml");

        return environment;
    }

    private void addYamlPropertySources(
            YamlPropertySourceLoader loader,
            MutablePropertySources propertySources,
            String resourcePath
    ) throws Exception {
        FileSystemResource resource = new FileSystemResource("src/main/resources/" + resourcePath);
        assertThat(resource.exists())
                .as("Expected main application config to exist: %s", resource.getPath())
                .isTrue();

        List<PropertySource<?>> loadedSources = loader.load(resourcePath, resource);

        for (PropertySource<?> propertySource : loadedSources) {
            propertySources.addLast(propertySource);
        }
    }
}
