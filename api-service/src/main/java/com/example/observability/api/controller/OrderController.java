package com.example.observability.api.controller;

import com.example.observability.api.service.OrderService;
import com.example.observability.model.Order;
import com.example.observability.model.OrderStatus;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService orderService;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @PostMapping
    @Timed(value = "http.requests", description = "HTTP requests", extraTags = {"endpoint", "create-order"})
    public ResponseEntity<?> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        logger.info("Received request to create order for customer: {} with amount: {}", 
            request.customerId(), request.amount());
        
        var result = orderService.createOrder(request.customerId(), request.amount());
        
        if (result.isSuccess()) {
            var order = result.getValue().orElseThrow();
            logger.info("Order created successfully: {}", order.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } else {
            logger.warn("Order creation failed: {}", result.getErrors());
            return ResponseEntity.badRequest().body(new ErrorResponse(result.getErrors()));
        }
    }
    
    @GetMapping("/{orderId}")
    @Timed(value = "http.requests", description = "HTTP requests", extraTags = {"endpoint", "get-order"})
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {
        logger.debug("Received request to get order: {}", orderId);
        
        return orderService.findById(orderId)
            .map(order -> {
                logger.debug("Order found: {}", orderId);
                return ResponseEntity.ok(order);
            })
            .orElseGet(() -> {
                logger.warn("Order not found: {}", orderId);
                return ResponseEntity.notFound().build();
            });
    }
    
    @PutMapping("/{orderId}/status")
    @Timed(value = "http.requests", description = "HTTP requests", extraTags = {"endpoint", "update-order-status"})
    public ResponseEntity<?> updateOrderStatus(@PathVariable String orderId, 
                                              @RequestBody @Valid UpdateOrderStatusRequest request) {
        logger.info("Received request to update order {} status to: {}", orderId, request.status());
        
        var result = orderService.updateOrderStatus(orderId, request.status());
        
        if (result.isSuccess()) {
            var order = result.getValue().orElseThrow();
            logger.info("Order status updated successfully: {} -> {}", orderId, order.status());
            return ResponseEntity.ok(order);
        } else {
            logger.warn("Order status update failed for {}: {}", orderId, result.getErrors());
            return ResponseEntity.badRequest().body(new ErrorResponse(result.getErrors()));
        }
    }
    
    @GetMapping("/customer/{customerId}")
    @Timed(value = "http.requests", description = "HTTP requests", extraTags = {"endpoint", "get-orders-by-customer"})
    public ResponseEntity<Page<Order>> getOrdersByCustomer(@PathVariable String customerId,
                                                          @PageableDefault(size = 20) Pageable pageable) {
        logger.debug("Received request to get orders for customer: {}", customerId);
        
        var orders = orderService.findByCustomerId(customerId, pageable);
        logger.debug("Found {} orders for customer: {}", orders.getTotalElements(), customerId);
        
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/status/{status}")
    @Timed(value = "http.requests", description = "HTTP requests", extraTags = {"endpoint", "get-orders-by-status"})
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable OrderStatus status) {
        logger.debug("Received request to get orders by status: {}", status);
        
        var orders = orderService.findByStatus(status);
        logger.debug("Found {} orders with status: {}", orders.size(), status);
        
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/date-range")
    @Timed(value = "http.requests", description = "HTTP requests", extraTags = {"endpoint", "get-orders-by-date-range"})
    public ResponseEntity<List<Order>> getOrdersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        logger.debug("Received request to get orders between {} and {}", startDate, endDate);
        
        var orders = orderService.findByDateRange(startDate, endDate);
        logger.debug("Found {} orders in date range", orders.size());
        
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/stats/status/{status}/count")
    @Timed(value = "http.requests", description = "HTTP requests", extraTags = {"endpoint", "count-orders-by-status"})
    public ResponseEntity<Long> countOrdersByStatus(@PathVariable OrderStatus status) {
        logger.debug("Received request to count orders by status: {}", status);
        
        var count = orderService.countByStatus(status);
        logger.debug("Found {} orders with status: {}", count, status);
        
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/stats/customer/{customerId}/count")
    @Timed(value = "http.requests", description = "HTTP requests", extraTags = {"endpoint", "count-orders-by-customer"})
    public ResponseEntity<Long> countOrdersByCustomer(@PathVariable String customerId) {
        logger.debug("Received request to count orders for customer: {}", customerId);
        
        var count = orderService.countByCustomerId(customerId);
        logger.debug("Found {} orders for customer: {}", count, customerId);
        
        return ResponseEntity.ok(count);
    }
    
    // Request/Response DTOs
    public record CreateOrderRequest(
        @NotBlank(message = "Customer ID is required")
        String customerId,
        
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        BigDecimal amount
    ) {}
    
    public record UpdateOrderStatusRequest(
        @NotNull(message = "Status is required")
        OrderStatus status
    ) {}
    
    public record ErrorResponse(List<String> errors) {}
}