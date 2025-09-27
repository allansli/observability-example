package com.example.observability.api.repository;

import com.example.observability.model.Order;
import com.example.observability.model.OrderStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class OrderEntity {
    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OrderStatus status;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    protected OrderEntity() {
        // JPA constructor
    }
    
    public OrderEntity(String id, String customerId, BigDecimal amount, OrderStatus status) {
        this.id = id;
        this.customerId = customerId;
        this.amount = amount;
        this.status = status;
    }
    
    public static OrderEntity fromModel(Order order) {
        return new OrderEntity(
            order.id(),
            order.customerId(),
            order.amount(),
            order.status()
        );
    }
    
    public Order toModel() {
        return new Order(id, customerId, amount, status, createdAt, updatedAt);
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}