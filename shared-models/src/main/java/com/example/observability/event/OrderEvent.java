package com.example.observability.event;

import com.example.observability.model.Order;
import com.example.observability.model.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record OrderEvent(
    @JsonProperty("eventId")
    @NotBlank(message = "Event ID cannot be blank")
    String eventId,
    
    @JsonProperty("orderId")
    @NotBlank(message = "Order ID cannot be blank")
    @Size(max = 36, message = "Order ID must not exceed 36 characters")
    String orderId,
    
    @JsonProperty("customerId")
    @NotBlank(message = "Customer ID cannot be blank")
    @Size(max = 36, message = "Customer ID must not exceed 36 characters")
    String customerId,
    
    @JsonProperty("eventType")
    @NotNull(message = "Event type cannot be null")
    OrderEventType eventType,
    
    @JsonProperty("previousStatus")
    OrderStatus previousStatus,
    
    @JsonProperty("currentStatus")
    @NotNull(message = "Current status cannot be null")
    OrderStatus currentStatus,
    
    @JsonProperty("eventData")
    Map<String, Object> eventData,
    
    @JsonProperty("timestamp")
    @NotNull(message = "Timestamp cannot be null")
    Instant timestamp,
    
    @JsonProperty("traceId")
    String traceId,
    
    @JsonProperty("spanId")
    String spanId,
    
    @JsonProperty("source")
    @NotBlank(message = "Source cannot be blank")
    String source
) {
    
    public static OrderEvent orderCreated(Order order, String source) {
        return new OrderEvent(
            UUID.randomUUID().toString(),
            order.id(),
            order.customerId(),
            OrderEventType.ORDER_CREATED,
            null,
            order.status(),
            Map.of("amount", order.amount().toString()),
            Instant.now(),
            null, // Will be populated by observability context
            null, // Will be populated by observability context
            source
        );
    }
    
    public static OrderEvent orderStatusChanged(Order order, OrderStatus previousStatus, String source) {
        return new OrderEvent(
            UUID.randomUUID().toString(),
            order.id(),
            order.customerId(),
            OrderEventType.ORDER_STATUS_CHANGED,
            previousStatus,
            order.status(),
            Map.of(
                "amount", order.amount().toString(),
                "previousStatus", previousStatus.getValue(),
                "newStatus", order.status().getValue()
            ),
            Instant.now(),
            null, // Will be populated by observability context
            null, // Will be populated by observability context
            source
        );
    }
    
    public static OrderEvent orderProcessed(Order order, String source, Map<String, Object> processingData) {
        return new OrderEvent(
            UUID.randomUUID().toString(),
            order.id(),
            order.customerId(),
            OrderEventType.ORDER_PROCESSED,
            OrderStatus.PROCESSING,
            order.status(),
            processingData,
            Instant.now(),
            null, // Will be populated by observability context
            null, // Will be populated by observability context
            source
        );
    }
    
    public OrderEvent withTraceContext(String traceId, String spanId) {
        return new OrderEvent(
            eventId,
            orderId,
            customerId,
            eventType,
            previousStatus,
            currentStatus,
            eventData,
            timestamp,
            traceId,
            spanId,
            source
        );
    }
    
    public boolean hasTraceContext() {
        return traceId != null && spanId != null;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OrderEvent that = (OrderEvent) obj;
        return Objects.equals(eventId, that.eventId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }
    
    @Override
    public String toString() {
        return "OrderEvent{" +
               "eventId='" + eventId + '\'' +
               ", orderId='" + orderId + '\'' +
               ", customerId='" + customerId + '\'' +
               ", eventType=" + eventType +
               ", previousStatus=" + previousStatus +
               ", currentStatus=" + currentStatus +
               ", timestamp=" + timestamp +
               ", traceId='" + traceId + '\'' +
               ", spanId='" + spanId + '\'' +
               ", source='" + source + '\'' +
               '}';
    }
}