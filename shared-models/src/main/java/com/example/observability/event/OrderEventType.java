package com.example.observability.event;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderEventType {
    ORDER_CREATED("ORDER_CREATED"),
    ORDER_STATUS_CHANGED("ORDER_STATUS_CHANGED"),
    ORDER_PROCESSED("ORDER_PROCESSED"),
    ORDER_VALIDATION_FAILED("ORDER_VALIDATION_FAILED"),
    ORDER_PROCESSING_FAILED("ORDER_PROCESSING_FAILED");
    
    private final String value;
    
    OrderEventType(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    public static OrderEventType fromValue(String value) {
        for (OrderEventType type : OrderEventType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown OrderEventType value: " + value);
    }
    
    public boolean isErrorEvent() {
        return this == ORDER_VALIDATION_FAILED || this == ORDER_PROCESSING_FAILED;
    }
    
    public boolean isStatusChangeEvent() {
        return this == ORDER_STATUS_CHANGED;
    }
    
    public boolean isCreationEvent() {
        return this == ORDER_CREATED;
    }
    
    public boolean isProcessingEvent() {
        return this == ORDER_PROCESSED;
    }
    
    @Override
    public String toString() {
        return value;
    }
}