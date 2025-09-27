package com.example.observability.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ValidationUtils {
    
    private ValidationUtils() {
        // Utility class
    }
    
    public static <T> ValidationResult<T> validate(T object, Validator validator) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        
        if (violations.isEmpty()) {
            return ValidationResult.success(object);
        }
        
        List<String> errors = violations.stream()
            .map(ConstraintViolation::getMessage)
            .sorted()
            .collect(Collectors.toList());
            
        return ValidationResult.failure(errors);
    }
    
    public static <T> ValidationResult<T> validate(T object, Validator validator, Class<?>... groups) {
        Set<ConstraintViolation<T>> violations = validator.validate(object, groups);
        
        if (violations.isEmpty()) {
            return ValidationResult.success(object);
        }
        
        List<String> errors = violations.stream()
            .map(ConstraintViolation::getMessage)
            .sorted()
            .collect(Collectors.toList());
            
        return ValidationResult.failure(errors);
    }
    
    public static boolean isValidUUID(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return false;
        }
        
        try {
            java.util.UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        
        // Basic email validation pattern
        String emailPattern = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
        return email.matches(emailPattern);
    }
    
    public static boolean isPositiveAmount(java.math.BigDecimal amount) {
        return amount != null && amount.compareTo(java.math.BigDecimal.ZERO) > 0;
    }
}