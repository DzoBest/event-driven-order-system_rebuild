package com.eventdriven.order_service.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    @Override
    @EntityGraph(attributePaths = "items")
    java.util.Optional<Order> findById(UUID id);
}