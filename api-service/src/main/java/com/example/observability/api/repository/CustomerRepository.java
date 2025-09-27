package com.example.observability.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, String> {
    
    Optional<CustomerEntity> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT c FROM CustomerEntity c WHERE c.id = :id")
    Optional<CustomerEntity> findByIdWithQuery(@Param("id") String id);
}