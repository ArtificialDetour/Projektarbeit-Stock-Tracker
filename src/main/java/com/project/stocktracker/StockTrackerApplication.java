package com.project.stocktracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * Starts the stock tracker Spring Boot application.
 */
@SpringBootApplication
public class StockTrackerApplication {

	/**
	 * Boots the Spring application context.
	 */
	public static void main(String[] args) {
		SpringApplication.run(StockTrackerApplication.class, args);
	}

	/**
	 * Provides the HTTP client used by external API services.
	 */
	@org.springframework.context.annotation.Bean
	public org.springframework.web.client.RestTemplate restTemplate() {
		return new org.springframework.web.client.RestTemplate();
	}

	/**
	 * Provides the shared JSON mapper for services and controllers.
	 */
	@org.springframework.context.annotation.Bean
	public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
		return new com.fasterxml.jackson.databind.ObjectMapper();
	}
}
