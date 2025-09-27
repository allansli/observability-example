package com.example.observability.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {
    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    PROCESSED("PROCESSED"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED"),
    CANCELLED("CANCELLED");
    
    private final String value;
    
    OrderStatus(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    public static OrderStatus fromValue(String value) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown OrderStatus value: " + value);
    }
    
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
    
    public boolean canTransitionTo(OrderStatus newStatus) {
        return switch (this) {
            case PENDING -> newStatus == PROCESSING || newStatus == CANCELLED;
            case PROCESSING -> newStatus == PROCESSED || newStatus == COMPLETED || newStatus == FAILED;
            case PROCESSED -> newStatus == COMPLETED || newStatus == FAILED;
            case COMPLETED, FAILED, CANCELLED -> false;
        };
    }
    
    @Override
    public String toString() {
        return value;
    }
}