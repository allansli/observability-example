package com.example.observability.subscriber.repository;

import com.example.observability.model.OrderStatus;
import com.example.observability.subscriber.entity.OrderEntity;
import io.micrometer.observation.annotation.Observed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for order database operations in the subscriber service.
 * 
 * Provides methods for order status updates and queries with comprehensive
 * observability through Spring Data JPA and Micrometer.
 */
@Repository
@Observed(name = "order.repository", contextualName = "order-repository")
public interface OrderRepository extends JpaRepository<OrderEntity, String> {

    /**
     * Update order status by ID with optimistic locking.
     * 
     * @param orderId the order ID
     * @param newStatus the new status
     * @param version the current version for optimistic locking
     * @return number of rows affected
     */
    @Modifying
    @Query("UPDATE OrderEntity o SET o.status = :status, o.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE o.id = :orderId AND o.version = :version")
    int updateOrderStatus(@Param("orderId") String orderId, 
                         @Param("status") OrderStatus newStatus, 
                         @Param("version") Long version);

    /**
     * Find orders by status for monitoring and reporting.
     * 
     * @param status the order status
     * @return list of orders with the specified status
     */
    @Query("SELECT o FROM OrderEntity o WHERE o.status = :status ORDER BY o.createdAt DESC")
    List<OrderEntity> findByStatus(@Param("status") OrderStatus status);

    /**
     * Find orders by customer ID and status.
     * 
     * @param customerId the customer ID
     * @param status the order status
     * @return list of orders for the customer with the specified status
     */
    @Query("SELECT o FROM OrderEntity o WHERE o.customerId = :customerId AND o.status = :status " +
           "ORDER BY o.createdAt DESC")
    List<OrderEntity> findByCustomerIdAndStatus(@Param("customerId") String customerId, 
                                                @Param("status") OrderStatus status);

    /**
     * Find orders created after a specific timestamp.
     * Used for monitoring recent order processing.
     * 
     * @param createdAfter the timestamp threshold
     * @return list of recent orders
     */
    @Query("SELECT o FROM OrderEntity o WHERE o.createdAt > :createdAfter ORDER BY o.createdAt DESC")
    List<OrderEntity> findOrdersCreatedAfter(@Param("createdAfter") Instant createdAfter);

    /**
     * Count orders by status for metrics collection.
     * 
     * @param status the order status
     * @return count of orders with the specified status
     */
    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);

    /**
     * Find order with version for optimistic locking support.
     * 
     * @param orderId the order ID
     * @return optional order entity with version
     */
    @Query("SELECT o FROM OrderEntity o WHERE o.id = :orderId")
    Optional<OrderEntity> findByIdWithVersion(@Param("orderId") String orderId);
}