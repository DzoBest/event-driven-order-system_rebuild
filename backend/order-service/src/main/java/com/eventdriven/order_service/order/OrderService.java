package com.eventdriven.order_service.order;

import com.eventdriven.order_service.api.OrderRequest;
import com.eventdriven.order_service.outbox.OutboxEvent;
import com.eventdriven.order_service.outbox.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository, OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Order create(OrderRequest request) {
        UUID orderId = UUID.randomUUID();
        List<OrderItem> items = request.items().stream()
                .map(item -> new OrderItem(UUID.randomUUID(), item.productId(), item.quantity()))
                .toList();
        Order order = new Order(orderId, request.customerId(), items);
        orderRepository.save(order);

        try {
            var event = new OrderCreatedEvent(UUID.randomUUID(), "OrderCreated",
                    OffsetDateTime.now(), 1, orderId,
                    new OrderCreatedPayload(request.customerId(), request.items()));
            outboxEventRepository.save(new OutboxEvent(UUID.randomUUID(), orderId, "OrderCreated",
                    objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize order created event", exception);
        }
        return order;
    }

    @Transactional
    public Order get(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    public record OrderCreatedEvent(UUID eventId, String eventType, OffsetDateTime occurredAt,
                                     int version, UUID orderId, OrderCreatedPayload payload) {}
    
    public record OrderCreatedPayload(String customerId, List<OrderRequest.ItemRequest> items) {}
}