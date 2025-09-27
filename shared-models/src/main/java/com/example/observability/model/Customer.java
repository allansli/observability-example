package com.example.observability.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Objects;

public record Customer(
    @JsonProperty("id")
    @NotBlank(message = "Customer ID cannot be blank")
    @Size(max = 36, message = "Customer ID must not exceed 36 characters")
    String id,
    
    @JsonProperty("name")
    @NotBlank(message = "Customer name cannot be blank")
    @Size(max = 255, message = "Customer name must not exceed 255 characters")
    String name,
    
    @JsonProperty("email")
    @NotBlank(message = "Customer email cannot be blank")
    @Email(message = "Customer email must be valid")
    @Size(max = 255, message = "Customer email must not exceed 255 characters")
    String email,
    
    @JsonProperty("createdAt")
    Instant createdAt,
    
    @JsonProperty("updatedAt")
    Instant updatedAt
) {
    
    public static Customer create(String id, String name, String email) {
        var now = Instant.now();
        return new Customer(id, name, email, now, now);
    }
    
    public Customer withUpdatedTimestamp() {
        return new Customer(id, name, email, createdAt, Instant.now());
    }
    
    public Customer withName(String newName) {
        return new Customer(id, newName, email, createdAt, Instant.now());
    }
    
    public Customer withEmail(String newEmail) {
        return new Customer(id, name, newEmail, createdAt, Instant.now());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Customer customer = (Customer) obj;
        return Objects.equals(id, customer.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Customer{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", email='" + email + '\'' +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               '}';
    }
}