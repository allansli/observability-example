package com.example.observability.event;

import com.example.observability.model.Order;
import com.example.observability.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderEventTest {
    
    @Test
    void shouldCreateOrderCreatedEvent() {
        var order = Order.create("order-123", "customer-456", new BigDecimal("99.99"));
        var event = OrderEvent.orderCreated(order, "api-service");
        
        assertNotNull(event);
        assertNotNull(event.eventId());
        assertEquals("order-123", event.orderId());
        assertEquals("customer-456", event.customerId());
        assertEquals(OrderEventType.ORDER_CREATED, event.eventType());
        assertNull(event.previousStatus());
        assertEquals(OrderStatus.PENDING, event.currentStatus());
        assertNotNull(event.eventData());
        assertEquals("99.99", event.eventData().get("amount"));
        assertEquals(order.createdAt(), event.timestamp());
        assertEquals("api-service", event.source());
    }
    
    @Test
    void shouldCreateOrderStatusChangedEvent() {
        var originalOrder = Order.create("order-123", "customer-456", new BigDecimal("99.99"));
        var updatedOrder = originalOrder.withStatus(OrderStatus.PROCESSING);
        var event = OrderEvent.orderStatusChanged(updatedOrder, OrderStatus.PENDING, "api-service");
        
        assertEquals("order-123", event.orderId());
        assertEquals("customer-456", event.customerId());
        assertEquals(OrderEventType.ORDER_STATUS_CHANGED, event.eventType());
        assertEquals(OrderStatus.PENDING, event.previousStatus());
        assertEquals(OrderStatus.PROCESSING, event.currentStatus());
        assertEquals("99.99", event.eventData().get("amount"));
        assertEquals("PENDING", event.eventData().get("previousStatus"));
        assertEquals("PROCESSING", event.eventData().get("newStatus"));
        assertEquals("api-service", event.source());
    }
    
    @Test
    void shouldAddTraceContext() {
        var order = Order.create("order-123", "customer-456", new BigDecimal("99.99"));
        var originalEvent = OrderEvent.orderCreated(order, "api-service");
        var eventWithTrace = originalEvent.withTraceContext("trace-123", "span-456");
        
        assertEquals("trace-123", eventWithTrace.traceId());
        assertEquals("span-456", eventWithTrace.spanId());
        assertTrue(eventWithTrace.hasTraceContext());
        
        // Original event should remain unchanged
        assertNull(originalEvent.traceId());
        assertNull(originalEvent.spanId());
        assertFalse(originalEvent.hasTraceContext());
    }
    
    @Test
    void shouldMaintainEqualityBasedOnEventId() {
        var order = Order.create("order-123", "customer-456", new BigDecimal("99.99"));
        var event1 = OrderEvent.orderCreated(order, "api-service");
        var event2 = OrderEvent.orderCreated(order, "subscriber-service");
        
        assertNotEquals(event1, event2); // Different event IDs
        assertNotEquals(event1.hashCode(), event2.hashCode());
        
        // Same event with trace context should still be equal to itself
        var eventWithTrace = event1.withTraceContext("trace-123", "span-456");
        assertEquals(event1.eventId(), eventWithTrace.eventId());
    }
}