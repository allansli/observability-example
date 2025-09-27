package com.example.observability.subscriber.controller;

import com.example.observability.subscriber.listener.OrderEventListener;
import com.example.observability.subscriber.service.OrderProcessingService;
import io.micrometer.observation.annotation.Observed;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Health check controller for the subscriber service.
 * 
 * Provides health status and metrics information for monitoring
 * and observability purposes.
 */
@RestController
@RequestMapping("/health")
@Observed(name = "health.controller", contextualName = "health-controller")
public class HealthController {

    private final OrderProcessingService orderProcessingService;
    private final OrderEventListener orderEventListener;

    public HealthController(OrderProcessingService orderProcessingService, 
                           OrderEventListener orderEventListener) {
        this.orderProcessingService = orderProcessingService;
        this.orderEventListener = orderEventListener;
    }

    /**
     * Basic health check endpoint.
     * 
     * @return health status response
     */
    @GetMapping
    @Observed(name = "health.check", contextualName = "health-check")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "service", "subscriber-service",
            "timestamp", Instant.now(),
            "version", "1.0.0-SNAPSHOT"
        );
        
        return ResponseEntity.ok(health);
    }

    /**
     * Detailed health check with metrics.
     * 
     * @return detailed health status with processing metrics
     */
    @GetMapping("/detailed")
    @Observed(name = "health.detailed", contextualName = "health-detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "service", "subscriber-service",
            "timestamp", Instant.now(),
            "version", "1.0.0-SNAPSHOT",
            "metrics", Map.of(
                "orderProcessing", orderProcessingService.getProcessingMetrics(),
                "messageListener", orderEventListener.getListenerMetrics()
            )
        );
        
        return ResponseEntity.ok(health);
    }
}