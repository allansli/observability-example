package com.example.observability.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record Order(
    @JsonProperty("id")
    @NotBlank(message = "Order ID cannot be blank")
    @Size(max = 36, message = "Order ID must not exceed 36 characters")
    String id,
    
    @JsonProperty("customerId")
    @NotBlank(message = "Customer ID cannot be blank")
    @Size(max = 36, message = "Customer ID must not exceed 36 characters")
    String customerId,
    
    @JsonProperty("amount")
    @NotNull(message = "Order amount cannot be null")
    @DecimalMin(value = "0.00", message = "Order amount must be positive")
    @Digits(integer = 8, fraction = 2, message = "Order amount must have at most 8 integer digits and 2 decimal places")
    BigDecimal amount,
    
    @JsonProperty("status")
    @NotNull(message = "Order status cannot be null")
    OrderStatus status,
    
    @JsonProperty("createdAt")
    Instant createdAt,
    
    @JsonProperty("updatedAt")
    Instant updatedAt
) {
    
    public static Order create(String id, String customerId, BigDecimal amount) {
        var now = Instant.now();
        return new Order(id, customerId, amount, OrderStatus.PENDING, now, now);
    }
    
    public Order withStatus(OrderStatus newStatus) {
        return new Order(id, customerId, amount, newStatus, createdAt, Instant.now());
    }
    
    public Order withAmount(BigDecimal newAmount) {
        return new Order(id, customerId, newAmount, status, createdAt, Instant.now());
    }
    
    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }
    
    public boolean isProcessing() {
        return status == OrderStatus.PROCESSING;
    }
    
    public boolean isCompleted() {
        return status == OrderStatus.COMPLETED;
    }
    
    public boolean isFailed() {
        return status == OrderStatus.FAILED;
    }
    
    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Order order = (Order) obj;
        return Objects.equals(id, order.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Order{" +
               "id='" + id + '\'' +
               ", customerId='" + customerId + '\'' +
               ", amount=" + amount +
               ", status=" + status +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               '}';
    }
}