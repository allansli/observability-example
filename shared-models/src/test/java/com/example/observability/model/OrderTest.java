package com.example.observability.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {
    
    @Test
    void shouldCreateOrderWithPendingStatus() {
        var order = Order.create("order-123", "customer-456", new BigDecimal("99.99"));
        
        assertNotNull(order);
        assertEquals("order-123", order.id());
        assertEquals("customer-456", order.customerId());
        assertEquals(new BigDecimal("99.99"), order.amount());
        assertEquals(OrderStatus.PENDING, order.status());
        assertNotNull(order.createdAt());
        assertNotNull(order.updatedAt());
        assertEquals(order.createdAt(), order.updatedAt());
    }
    
    @Test
    void shouldUpdateOrderStatus() {
        var originalOrder = Order.create("order-123", "customer-456", new BigDecimal("99.99"));
        var updatedOrder = originalOrder.withStatus(OrderStatus.PROCESSING);
        
        assertEquals("order-123", updatedOrder.id());
        assertEquals("customer-456", updatedOrder.customerId());
        assertEquals(new BigDecimal("99.99"), updatedOrder.amount());
        assertEquals(OrderStatus.PROCESSING, updatedOrder.status());
        assertEquals(originalOrder.createdAt(), updatedOrder.createdAt());
        assertTrue(updatedOrder.updatedAt().isAfter(originalOrder.updatedAt()) || 
                   updatedOrder.updatedAt().equals(originalOrder.updatedAt()));
    }
    
    @Test
    void shouldCheckOrderStatusMethods() {
        var pendingOrder = Order.create("order-123", "customer-456", new BigDecimal("99.99"));
        assertTrue(pendingOrder.isPending());
        assertFalse(pendingOrder.isProcessing());
        assertFalse(pendingOrder.isCompleted());
        assertFalse(pendingOrder.isFailed());
        assertFalse(pendingOrder.isCancelled());
        
        var processingOrder = pendingOrder.withStatus(OrderStatus.PROCESSING);
        assertFalse(processingOrder.isPending());
        assertTrue(processingOrder.isProcessing());
        assertFalse(processingOrder.isCompleted());
        assertFalse(processingOrder.isFailed());
        assertFalse(processingOrder.isCancelled());
        
        var completedOrder = processingOrder.withStatus(OrderStatus.COMPLETED);
        assertFalse(completedOrder.isPending());
        assertFalse(completedOrder.isProcessing());
        assertTrue(completedOrder.isCompleted());
        assertFalse(completedOrder.isFailed());
        assertFalse(completedOrder.isCancelled());
    }
    
    @Test
    void shouldMaintainEqualityBasedOnId() {
        var order1 = Order.create("order-123", "customer-456", new BigDecimal("99.99"));
        var order2 = Order.create("order-123", "customer-789", new BigDecimal("199.99"));
        var order3 = Order.create("order-456", "customer-456", new BigDecimal("99.99"));
        
        assertEquals(order1, order2); // Same ID
        assertNotEquals(order1, order3); // Different ID
        assertEquals(order1.hashCode(), order2.hashCode());
        assertNotEquals(order1.hashCode(), order3.hashCode());
    }
}