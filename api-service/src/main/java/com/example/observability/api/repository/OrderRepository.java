package com.example.observability.api.repository;

import com.example.observability.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    
    List<OrderEntity> findByCustomerId(String customerId);
    
    Page<OrderEntity> findByCustomerId(String customerId, Pageable pageable);
    
    List<OrderEntity> findByStatus(OrderStatus status);
    
    Page<OrderEntity> findByStatus(OrderStatus status, Pageable pageable);
    
    List<OrderEntity> findByCustomerIdAndStatus(String customerId, OrderStatus status);
    
    @Query("SELECT o FROM OrderEntity o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate")
    List<OrderEntity> findByCreatedAtBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);
    
    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.customerId = :customerId")
    long countByCustomerId(@Param("customerId") String customerId);
}