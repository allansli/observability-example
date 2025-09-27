package com.example.observability.subscriber;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Main application class for the Subscriber Service.
 * 
 * This service processes order events asynchronously from Google Cloud Pub/Sub,
 * demonstrating comprehensive observability with OpenTelemetry and Micrometer.
 * 
 * Features:
 * - Java 21 virtual threads for high-performance async processing
 * - Spring Boot 3.2+ auto-configuration
 * - OpenTelemetry distributed tracing
 * - Micrometer metrics collection
 * - Structured logging with correlation IDs
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SubscriberServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubscriberServiceApplication.class, args);
    }
}