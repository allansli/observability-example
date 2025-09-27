package com.example.observability.api.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import jakarta.validation.Validator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {
    
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
    
    @Bean
    public HealthIndicator customHealthIndicator() {
        return () -> Health.up()
            .withDetail("service", "api-service")
            .withDetail("version", "1.0.0")
            .withDetail("status", "operational")
            .build();
    }
    
    @Bean
    public Validator validator() {
        return jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
    }
}