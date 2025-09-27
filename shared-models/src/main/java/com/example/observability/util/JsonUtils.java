package com.example.observability.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.util.Optional;

public final class JsonUtils {
    
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();
    
    private JsonUtils() {
        // Utility class
    }
    
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
    
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
    
    public static String toJson(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonSerializationException("Failed to serialize object to JSON: " + object.getClass().getSimpleName(), e);
        }
    }
    
    public static Optional<String> toJsonSafe(Object object) {
        try {
            return Optional.of(OBJECT_MAPPER.writeValueAsString(object));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }
    
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new JsonDeserializationException("Failed to deserialize JSON to " + clazz.getSimpleName(), e);
        }
    }
    
    public static <T> Optional<T> fromJsonSafe(String json, Class<T> clazz) {
        try {
            return Optional.of(OBJECT_MAPPER.readValue(json, clazz));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }
    
    public static <T> T fromJson(byte[] json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            throw new JsonDeserializationException("Failed to deserialize JSON bytes to " + clazz.getSimpleName(), e);
        }
    }
    
    public static <T> Optional<T> fromJsonSafe(byte[] json, Class<T> clazz) {
        try {
            return Optional.of(OBJECT_MAPPER.readValue(json, clazz));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
    
    public static boolean isValidJson(String json) {
        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
    
    public static String toPrettyJson(Object object) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonSerializationException("Failed to serialize object to pretty JSON: " + object.getClass().getSimpleName(), e);
        }
    }
    
    public static class JsonSerializationException extends RuntimeException {
        public JsonSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class JsonDeserializationException extends RuntimeException {
        public JsonDeserializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}