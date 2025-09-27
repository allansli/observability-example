package com.example.observability.util;

import java.util.List;

public class ValidationException extends RuntimeException {
    
    private final List<String> errors;
    
    public ValidationException(String message) {
        super(message);
        this.errors = List.of(message);
    }
    
    public ValidationException(List<String> errors) {
        super("Validation failed: " + String.join(", ", errors));
        this.errors = List.copyOf(errors);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.errors = List.of(message);
    }
    
    public ValidationException(List<String> errors, Throwable cause) {
        super("Validation failed: " + String.join(", ", errors), cause);
        this.errors = List.copyOf(errors);
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public boolean hasMultipleErrors() {
        return errors.size() > 1;
    }
    
    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }
}