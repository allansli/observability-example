package com.example.observability.api.service;

import com.example.observability.api.repository.CustomerEntity;
import com.example.observability.api.repository.CustomerRepository;
import com.example.observability.model.Customer;
import com.example.observability.util.ValidationResult;
import com.example.observability.util.ValidationUtils;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CustomerService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);
    
    private final CustomerRepository customerRepository;
    private final Validator validator;
    
    // Metrics
    private final Counter customersCreatedCounter;
    private final Counter customerValidationFailuresCounter;
    
    public CustomerService(CustomerRepository customerRepository,
                          Validator validator,
                          MeterRegistry meterRegistry) {
        this.customerRepository = customerRepository;
        this.validator = validator;
        
        // Initialize metrics
        this.customersCreatedCounter = Counter.builder("customers.created")
            .description("Number of customers created")
            .tag("service", "api-service")
            .register(meterRegistry);
            
        this.customerValidationFailuresCounter = Counter.builder("customers.validation.failures")
            .description("Number of customer validation failures")
            .tag("service", "api-service")
            .register(meterRegistry);
    }
    
    @Timed(value = "customers.create", description = "Time taken to create a customer")
    public ValidationResult<Customer> createCustomer(String id, String name, String email) {
        logger.info("Creating customer: {} with email: {}", name, email);
        
        // Check if email already exists
        if (customerRepository.existsByEmail(email)) {
            customerValidationFailuresCounter.increment();
            return ValidationResult.failure("Customer with email already exists: " + email);
        }
        
        // Create customer
        var customer = Customer.create(id, name, email);
        
        // Validate customer
        var validationResult = ValidationUtils.validate(customer, validator);
        if (validationResult.isFailure()) {
            customerValidationFailuresCounter.increment();
            return validationResult;
        }
        
        // Save to database
        var customerEntity = CustomerEntity.fromModel(customer);
        var savedEntity = customerRepository.save(customerEntity);
        var savedCustomer = savedEntity.toModel();
        
        customersCreatedCounter.increment();
        logger.info("Customer created successfully: {}", savedCustomer.id());
        
        return ValidationResult.success(savedCustomer);
    }
    
    @Transactional(readOnly = true)
    public Optional<Customer> findById(String customerId) {
        logger.debug("Finding customer by ID: {}", customerId);
        return customerRepository.findById(customerId)
            .map(CustomerEntity::toModel);
    }
    
    @Transactional(readOnly = true)
    public Optional<Customer> findByEmail(String email) {
        logger.debug("Finding customer by email: {}", email);
        return customerRepository.findByEmail(email)
            .map(CustomerEntity::toModel);
    }
    
    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        logger.debug("Finding all customers");
        return customerRepository.findAll()
            .stream()
            .map(CustomerEntity::toModel)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public boolean existsById(String customerId) {
        return customerRepository.existsById(customerId);
    }
    
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return customerRepository.existsByEmail(email);
    }
}