package com.example.observability.api.controller;

import com.example.observability.api.service.CustomerService;
import com.example.observability.model.Customer;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);
    
    private final CustomerService customerService;
    
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }
    
    @PostMapping
    @Timed(value = "http.requests", description = "HTTP requests", extraTags = {"endpoint", "create-customer"})
    public ResponseEntity<?> createCustomer(@RequestBody @Valid CreateCustomerRequest request) {
        logger.info("Received request to create customer: {} with email: {}", 
            request.name(), request.email());
        
        var customerId = UUID.randomUUID().toString();
        var result = customerService.createCustomer(customerId, request.name(), request.email());
        
        if (result.isSuccess()) {
            var customer = result.getValue().orElseThrow();
            logger.info("Customer created successfully: {}", customer.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(customer);
        } else {
            logger.warn("Customer creation failed: {}", result.getErrors());
            return ResponseEntity.badRequest().body(new ErrorResponse(result.getErrors()));
        }
    }
    
    @GetMapping("/{customerId}")
    @Timed(value = "http.requests", description = "HTTP requests", extraTags = {"endpoint", "get-customer"})
    public ResponseEntity<?> getCustomer(@PathVariable String customerId) {
        logger.debug("Received request to get customer: {}", customerId);
        
        return customerService.findById(customerId)
            .map(customer -> {
                logger.debug("Customer found: {}", customerId);
                return ResponseEntity.ok(customer);
            })
            .orElseGet(() -> {
                logger.warn("Customer not found: {}", customerId);
                return ResponseEntity.notFound().build();
            });
    }
    
    @GetMapping("/email/{email}")
    @Timed(value = "http.requests", description = "HTTP requests", extraTags = {"endpoint", "get-customer-by-email"})
    public ResponseEntity<?> getCustomerByEmail(@PathVariable @Email String email) {
        logger.debug("Received request to get customer by email: {}", email);
        
        return customerService.findByEmail(email)
            .map(customer -> {
                logger.debug("Customer found by email: {}", email);
                return ResponseEntity.ok(customer);
            })
            .orElseGet(() -> {
                logger.warn("Customer not found by email: {}", email);
                return ResponseEntity.notFound().build();
            });
    }
    
    @GetMapping
    @Timed(value = "http.requests", description = "HTTP requests", extraTags = {"endpoint", "get-all-customers"})
    public ResponseEntity<List<Customer>> getAllCustomers() {
        logger.debug("Received request to get all customers");
        
        var customers = customerService.findAll();
        logger.debug("Found {} customers", customers.size());
        
        return ResponseEntity.ok(customers);
    }
    
    @GetMapping("/{customerId}/exists")
    @Timed(value = "http.requests", description = "HTTP requests", extraTags = {"endpoint", "check-customer-exists"})
    public ResponseEntity<Boolean> checkCustomerExists(@PathVariable String customerId) {
        logger.debug("Received request to check if customer exists: {}", customerId);
        
        var exists = customerService.existsById(customerId);
        logger.debug("Customer {} exists: {}", customerId, exists);
        
        return ResponseEntity.ok(exists);
    }
    
    // Request/Response DTOs
    public record CreateCustomerRequest(
        @NotBlank(message = "Customer name is required")
        String name,
        
        @NotBlank(message = "Customer email is required")
        @Email(message = "Customer email must be valid")
        String email
    ) {}
    
    public record ErrorResponse(List<String> errors) {}
}