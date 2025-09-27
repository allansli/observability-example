package com.example.observability.api.service;

import com.example.observability.api.repository.CustomerRepository;
import com.example.observability.api.repository.OrderEntity;
import com.example.observability.api.repository.OrderRepository;
import com.example.observability.event.OrderEvent;
import com.example.observability.event.OrderEventType;
import com.example.observability.model.Order;
import com.example.observability.model.OrderStatus;
import com.example.observability.util.ValidationException;
import com.example.observability.util.ValidationResult;
import com.example.observability.util.ValidationUtils;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final PubSubService pubSubService;
    private final Validator validator;
    private final MeterRegistry meterRegistry;
    
    // Metrics
    private final Counter ordersCreatedCounter;
    private final Counter ordersUpdatedCounter;
    private final Counter orderValidationFailuresCounter;
    private final Timer orderCreationTimer;
    
    public OrderService(OrderRepository orderRepository,
                       CustomerRepository customerRepository,
                       PubSubService pubSubService,
                       Validator validator,
                       MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.pubSubService = pubSubService;
        this.validator = validator;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.ordersCreatedCounter = Counter.builder("orders.created")
            .description("Number of orders created")
            .tag("service", "api-service")
            .register(meterRegistry);
            
        this.ordersUpdatedCounter = Counter.builder("orders.updated")
            .description("Number of orders updated")
            .tag("service", "api-service")
            .register(meterRegistry);
            
        this.orderValidationFailuresCounter = Counter.builder("orders.validation.failures")
            .description("Number of order validation failures")
            .tag("service", "api-service")
            .register(meterRegistry);
            
        this.orderCreationTimer = Timer.builder("orders.creation.duration")
            .description("Time taken to create an order")
            .tag("service", "api-service")
            .register(meterRegistry);
    }
    
    @Timed(value = "orders.create", description = "Time taken to create an order")
    public ValidationResult<Order> createOrder(String customerId, BigDecimal amount) {
        logger.info("Creating order for customer: {} with amount: {}", customerId, amount);
        
        // Validate customer exists
        if (!customerRepository.existsById(customerId)) {
            orderValidationFailuresCounter.increment();
            return ValidationResult.failure("Customer not found: " + customerId);
        }
        
        // Create order
        var orderId = UUID.randomUUID().toString();
        var order = Order.create(orderId, customerId, amount);
        
        // Validate order
        var validationResult = ValidationUtils.validate(order, validator);
        if (validationResult.isFailure()) {
            orderValidationFailuresCounter.increment();
            return validationResult;
        }
        
        // Save to database
        var orderEntity = OrderEntity.fromModel(order);
        var savedEntity = orderRepository.save(orderEntity);
        var savedOrder = savedEntity.toModel();
        
        // Publish event
        publishOrderEvent(savedOrder, OrderEventType.ORDER_CREATED, null);
        
        ordersCreatedCounter.increment();
        logger.info("Order created successfully: {}", savedOrder.id());
        
        return ValidationResult.success(savedOrder);
    }
    
    @Timed(value = "orders.update.status", description = "Time taken to update order status")
    public ValidationResult<Order> updateOrderStatus(String orderId, OrderStatus newStatus) {
        logger.info("Updating order status: {} to {}", orderId, newStatus);
        
        return orderRepository.findById(orderId)
            .map(orderEntity -> {
                var currentOrder = orderEntity.toModel();
                var previousStatus = currentOrder.status();
                
                // Validate status transition
                if (!previousStatus.canTransitionTo(newStatus)) {
                    orderValidationFailuresCounter.increment();
                    return ValidationResult.<Order>failure(
                        "Invalid status transition from " + previousStatus + " to " + newStatus
                    );
                }
                
                // Update status
                var updatedOrder = currentOrder.withStatus(newStatus);
                var updatedEntity = OrderEntity.fromModel(updatedOrder);
                var savedEntity = orderRepository.save(updatedEntity);
                var savedOrder = savedEntity.toModel();
                
                // Publish event
                publishOrderEvent(savedOrder, OrderEventType.ORDER_STATUS_CHANGED, previousStatus);
                
                ordersUpdatedCounter.increment();
                logger.info("Order status updated successfully: {} -> {}", orderId, newStatus);
                
                return ValidationResult.success(savedOrder);
            })
            .orElseGet(() -> {
                orderValidationFailuresCounter.increment();
                return ValidationResult.failure("Order not found: " + orderId);
            });
    }
    
    @Transactional(readOnly = true)
    public Optional<Order> findById(String orderId) {
        logger.debug("Finding order by ID: {}", orderId);
        return orderRepository.findById(orderId)
            .map(OrderEntity::toModel);
    }
    
    @Transactional(readOnly = true)
    public List<Order> findByCustomerId(String customerId) {
        logger.debug("Finding orders for customer: {}", customerId);
        return orderRepository.findByCustomerId(customerId)
            .stream()
            .map(OrderEntity::toModel)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public Page<Order> findByCustomerId(String customerId, Pageable pageable) {
        logger.debug("Finding orders for customer: {} with pagination", customerId);
        return orderRepository.findByCustomerId(customerId, pageable)
            .map(OrderEntity::toModel);
    }
    
    @Transactional(readOnly = true)
    public List<Order> findByStatus(OrderStatus status) {
        logger.debug("Finding orders by status: {}", status);
        return orderRepository.findByStatus(status)
            .stream()
            .map(OrderEntity::toModel)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public List<Order> findByDateRange(Instant startDate, Instant endDate) {
        logger.debug("Finding orders between {} and {}", startDate, endDate);
        return orderRepository.findByCreatedAtBetween(startDate, endDate)
            .stream()
            .map(OrderEntity::toModel)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public long countByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }
    
    @Transactional(readOnly = true)
    public long countByCustomerId(String customerId) {
        return orderRepository.countByCustomerId(customerId);
    }
    
    private void publishOrderEvent(Order order, OrderEventType eventType, OrderStatus previousStatus) {
        try {
            OrderEvent event = switch (eventType) {
                case ORDER_CREATED -> OrderEvent.orderCreated(order, "api-service");
                case ORDER_STATUS_CHANGED -> OrderEvent.orderStatusChanged(order, previousStatus, "api-service");
                default -> throw new IllegalArgumentException("Unsupported event type: " + eventType);
            };
            
            pubSubService.publishOrderEvent(event);
            logger.debug("Published {} event for order: {}", eventType, order.id());
        } catch (Exception e) {
            logger.error("Failed to publish {} event for order: {}", eventType, order.id(), e);
            // Note: We don't rethrow here to avoid breaking the main transaction
        }
    }
}