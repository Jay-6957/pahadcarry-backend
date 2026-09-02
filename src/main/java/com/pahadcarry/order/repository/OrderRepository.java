package com.pahadcarry.order.repository;

import com.pahadcarry.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByCustomerIdOrderByPlacedAtDesc(String customerId);
    List<Order> findByStatus(String status);
    List<Order> findByBatchId(String batchId);

    @Query("SELECT o FROM Order o WHERE o.status = 'PLACED' AND o.placedAt <= :cutoff")
    List<Order> findOldUnbatchedOrders(Instant cutoff);

    long countByStatus(String status);
}
