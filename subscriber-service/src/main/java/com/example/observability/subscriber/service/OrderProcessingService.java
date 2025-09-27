package com.example.observability.subscriber.service;

import com.example.observability.event.OrderEvent;
import com.example.observability.model.OrderStatus;
import com.example.observability.subscriber.entity.OrderEntity;
import com.example.observability.subscriber.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Tags;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Service for processing order events received from Pub/Sub.
 * 
 * Demonstrates comprehensive observability with:
 * - Distributed tracing with OpenTelemetry
 * - Custom metrics with Micrometer
 * - Structured logging with correlation IDs
 * - Virtual threads for high-performance processing
 */
@Service
@Observed(name = "order.processing.service", contextualName = "order-processing")
public class OrderProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(OrderProcessingService.class);

    private final OrderRepository orderRepository;
    private final MeterRegistry meterRegistry;
    
    // Custom metrics
    private final Counter ordersProcessedCounter;
    private final Counter orderProcessingFailuresCounter;
    private final Counter optimisticLockFailuresCounter;
    private final Timer orderProcessingTimer;

    public OrderProcessingService(OrderRepository orderRepository, MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.meterRegistry = meterRegistry;
        
        // Initialize custom metrics
        this.ordersProcessedCounter = Counter.builder("orders.processed")
                .description("Number of orders successfully processed")
                .tag("service", "subscriber-service")
                .register(meterRegistry);
                
        this.orderProcessingFailuresCounter = Counter.builder("orders.processing.failures")
                .description("Number of order processing failures")
                .tag("service", "subscriber-service")
                .register(meterRegistry);
                
        this.optimisticLockFailuresCounter = Counter.builder("orders.optimistic.lock.failures")
                .description("Number of optimistic locking failures")
                .tag("service", "subscriber-service")
                .register(meterRegistry);
                
        this.orderProcessingTimer = Timer.builder("orders.processing.duration")
                .description("Time spent processing orders")
                .tag("service", "subscriber-service")
                .register(meterRegistry);
    }

    /**
     * Process an order event received from Pub/Sub.
     * 
     * @param orderEvent the order event to process
     * @return true if processing was successful, false otherwise
     */
    @Transactional
    @Observed(name = "order.processing.processOrderEvent", 
              contextualName = "process-order-event",
              lowCardinalityKeyValues = {"operation", "process"})
    public boolean processOrderEvent(OrderEvent orderEvent) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            logger.info("Processing order event: orderId={}, eventType={}, customerId={}", 
                       orderEvent.orderId(), orderEvent.eventType(), orderEvent.customerId());

            switch (orderEvent.eventType()) {
                case ORDER_CREATED -> {
                    return handleOrderCreated(orderEvent);
                }
                case ORDER_STATUS_CHANGED -> {
                    return handleOrderStatusChanged(orderEvent);
                }
                case ORDER_PROCESSED -> {
                    return handleOrderProcessed(orderEvent);
                }
                default -> {
                    logger.warn("Unknown order event type: {}", orderEvent.eventType());
                    orderProcessingFailuresCounter.increment();
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("Error processing order event: orderId={}, error={}", 
                        orderEvent.orderId(), e.getMessage(), e);
            orderProcessingFailuresCounter.increment();
            return false;
        } finally {
            sample.stop(orderProcessingTimer);
        }
    }

    /**
     * Handle ORDER_CREATED event by updating order status to PROCESSED.
     * 
     * @param orderEvent the order created event
     * @return true if successful
     */
    private boolean handleOrderCreated(OrderEvent orderEvent) {
        try {
            // Find the order with current version
            Optional<OrderEntity> orderOpt = orderRepository.findByIdWithVersion(orderEvent.orderId());
            
            if (orderOpt.isEmpty()) {
                logger.warn("Order not found for processing: orderId={}", orderEvent.orderId());
                orderProcessingFailuresCounter.increment();
                return false;
            }

            OrderEntity order = orderOpt.get();
            
            // Simulate some processing time
            simulateProcessing();
            
            // Update order status to PROCESSED
            int updatedRows = orderRepository.updateOrderStatus(
                order.getId(), 
                OrderStatus.PROCESSED, 
                order.getVersion()
            );
            
            if (updatedRows == 0) {
                logger.warn("Failed to update order status - optimistic locking failure: orderId={}", 
                           orderEvent.orderId());
                optimisticLockFailuresCounter.increment();
                return false;
            }
            
            logger.info("Order processed successfully: orderId={}, newStatus={}", 
                       orderEvent.orderId(), OrderStatus.PROCESSED);
            ordersProcessedCounter.increment();
            return true;
            
        } catch (OptimisticLockingFailureException e) {
            logger.warn("Optimistic locking failure for order: orderId={}", orderEvent.orderId());
            optimisticLockFailuresCounter.increment();
            return false;
        }
    }

    /**
     * Handle ORDER_STATUS_CHANGED event.
     * 
     * @param orderEvent the order status changed event
     * @return true if successful
     */
    private boolean handleOrderStatusChanged(OrderEvent orderEvent) {
        logger.info("Handling order status change: orderId={}, previousStatus={}, currentStatus={}", 
                   orderEvent.orderId(), orderEvent.previousStatus(), orderEvent.currentStatus());
        
        // For demonstration, just log the update
        // In a real scenario, this might trigger additional processing
        ordersProcessedCounter.increment();
        return true;
    }

    /**
     * Handle ORDER_PROCESSED event.
     * 
     * @param orderEvent the order processed event
     * @return true if successful
     */
    private boolean handleOrderProcessed(OrderEvent orderEvent) {
        try {
            Optional<OrderEntity> orderOpt = orderRepository.findByIdWithVersion(orderEvent.orderId());
            
            if (orderOpt.isEmpty()) {
                logger.warn("Order not found for processing: orderId={}", orderEvent.orderId());
                orderProcessingFailuresCounter.increment();
                return false;
            }

            OrderEntity order = orderOpt.get();
            
            // Update order to match the current status from the event
            int updatedRows = orderRepository.updateOrderStatus(
                order.getId(), 
                orderEvent.currentStatus(), 
                order.getVersion()
            );
            
            if (updatedRows == 0) {
                logger.warn("Failed to update order - optimistic locking failure: orderId={}", 
                           orderEvent.orderId());
                optimisticLockFailuresCounter.increment();
                return false;
            }
            
            logger.info("Order processed successfully: orderId={}, newStatus={}", 
                       orderEvent.orderId(), orderEvent.currentStatus());
            ordersProcessedCounter.increment();
            return true;
            
        } catch (OptimisticLockingFailureException e) {
            logger.warn("Optimistic locking failure for order processing: orderId={}", 
                       orderEvent.orderId());
            optimisticLockFailuresCounter.increment();
            return false;
        }
    }

    /**
     * Simulate processing time to demonstrate async behavior.
     * Uses virtual threads for efficient resource utilization.
     */
    private void simulateProcessing() {
        try {
            // Simulate some processing work (100-500ms)
            Thread.sleep(100 + (long) (Math.random() * 400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Processing simulation interrupted");
        }
    }

    /**
     * Get order processing metrics for health checks and monitoring.
     * 
     * @return processing metrics summary
     */
    @Observed(name = "order.processing.getMetrics", contextualName = "get-metrics")
    public String getProcessingMetrics() {
        return String.format(
            "OrderProcessingMetrics{processed=%d, failures=%d, lockFailures=%d}",
            (long) ordersProcessedCounter.count(),
            (long) orderProcessingFailuresCounter.count(),
            (long) optimisticLockFailuresCounter.count()
        );
    }
}