package com.example.observability.util;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract sealed class ValidationResult<T> permits ValidationResult.Success, ValidationResult.Failure {
    
    public static <T> ValidationResult<T> success(T value) {
        return new Success<>(value);
    }
    
    public static <T> ValidationResult<T> failure(List<String> errors) {
        return new Failure<>(errors);
    }
    
    public static <T> ValidationResult<T> failure(String error) {
        return new Failure<>(List.of(error));
    }
    
    public abstract boolean isSuccess();
    
    public abstract boolean isFailure();
    
    public abstract Optional<T> getValue();
    
    public abstract List<String> getErrors();
    
    public <U> ValidationResult<U> map(java.util.function.Function<T, U> mapper) {
        if (isSuccess()) {
            return success(mapper.apply(getValue().get()));
        } else {
            return failure(getErrors());
        }
    }
    
    public <U> ValidationResult<U> flatMap(java.util.function.Function<T, ValidationResult<U>> mapper) {
        if (isSuccess()) {
            return mapper.apply(getValue().get());
        } else {
            return failure(getErrors());
        }
    }
    
    public T orElse(T defaultValue) {
        return isSuccess() ? getValue().get() : defaultValue;
    }
    
    public T orElseThrow() {
        if (isSuccess()) {
            return getValue().get();
        } else {
            throw new ValidationException("Validation failed: " + String.join(", ", getErrors()));
        }
    }
    
    public T orElseThrow(java.util.function.Supplier<? extends RuntimeException> exceptionSupplier) {
        if (isSuccess()) {
            return getValue().get();
        } else {
            throw exceptionSupplier.get();
        }
    }
    
    public static final class Success<T> extends ValidationResult<T> {
        private final T value;
        
        private Success(T value) {
            this.value = Objects.requireNonNull(value);
        }
        
        @Override
        public boolean isSuccess() {
            return true;
        }
        
        @Override
        public boolean isFailure() {
            return false;
        }
        
        @Override
        public Optional<T> getValue() {
            return Optional.of(value);
        }
        
        @Override
        public List<String> getErrors() {
            return List.of();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Success<?> success = (Success<?>) obj;
            return Objects.equals(value, success.value);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
        
        @Override
        public String toString() {
            return "Success{value=" + value + '}';
        }
    }
    
    public static final class Failure<T> extends ValidationResult<T> {
        private final List<String> errors;
        
        private Failure(List<String> errors) {
            this.errors = Objects.requireNonNull(errors);
            if (errors.isEmpty()) {
                throw new IllegalArgumentException("Errors list cannot be empty for failure result");
            }
        }
        
        @Override
        public boolean isSuccess() {
            return false;
        }
        
        @Override
        public boolean isFailure() {
            return true;
        }
        
        @Override
        public Optional<T> getValue() {
            return Optional.empty();
        }
        
        @Override
        public List<String> getErrors() {
            return List.copyOf(errors);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Failure<?> failure = (Failure<?>) obj;
            return Objects.equals(errors, failure.errors);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(errors);
        }
        
        @Override
        public String toString() {
            return "Failure{errors=" + errors + '}';
        }
    }
}