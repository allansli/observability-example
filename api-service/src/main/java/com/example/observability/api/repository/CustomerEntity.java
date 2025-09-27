package com.example.observability.api.repository;

import com.example.observability.model.Customer;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "customers")
public class CustomerEntity {
    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    protected CustomerEntity() {
        // JPA constructor
    }
    
    public CustomerEntity(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
    
    public static CustomerEntity fromModel(Customer customer) {
        return new CustomerEntity(
            customer.id(),
            customer.name(),
            customer.email()
        );
    }
    
    public Customer toModel() {
        return new Customer(id, name, email, createdAt, updatedAt);
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}