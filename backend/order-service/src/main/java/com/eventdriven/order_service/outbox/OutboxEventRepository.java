package com.eventdriven.order_service.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
imort java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}