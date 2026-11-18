package com.eventdriven.order_service.stock;

import com.eventdriven.order_service.order.Order;
import com.eventdriven.order_service.order.OrderRepository;
import com.eventdriven.order_service.order.OrderStatus;
import com.eventdriven.order_service.processed.ProcessedEvent;
import com.eventdriven.order_service.processed.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class StockResultConsumer {
    private static final Logger log = LoggerFactory.getLogger(StockResultConsumer.class);
    private static final String CONSUMER_NAME = "order-status-consumer";

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public StockResultConsumer(
            OrderRepository orderRepository,
            ProcessedEventRepository processedEventRepository,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "orders.stock-validated",
            groupId = "order-status-validated-group"
    )
    public void onValidated(String message, Acknowledgment acknowledgment) {
        handle(message, acknowledgment, OrderStatus.STOCK_VALIDATED);
    }

    @KafkaListener(
            topics = "orders.stock-rejected",
            groupId = "order-status-rejected-group"
    )
    public void onRejected(String message, Acknowledgment acknowledgment) {
        handle(message, acknowledgment, OrderStatus.STOCK_NOT_AVAILABLE);
    }

    @Transactional
    public void handle(String message, Acknowledgment acknowledgment, OrderStatus status) {
        try {
            StockResultEvent event = objectMapper.readValue(message, StockResultEvent.class);
            UUID eventId = event.eventId();

            if (eventId == null || event.orderId() == null) {
                throw new IllegalArgumentException("Stock result event has no eventId or orderId");
            }

            if (processedEventRepository.existsByEventIdAndConsumerName(eventId, CONSUMER_NAME)) {
                log.info("Ignoring duplicate stock result event {}", eventId);
                acknowledgment.acknowledge();
                return;
            }

            Order order = orderRepository.findById(event.orderId()).orElse(null);
            if (order == null) {
                throw new IllegalArgumentException(
                        "Stock result references unknown order " + event.orderId()
                );
            }

            if (order.getStatus() == OrderStatus.PENDING) {
                order.updateStatus(status);
                orderRepository.save(order);
            }

            processedEventRepository.save(
                    new ProcessedEvent(eventId, CONSUMER_NAME)
            );
            acknowledgment.acknowledge();

            log.info(
                    "Updated order {} with status {} after event {}",
                    event.orderId(),
                    status,
                    eventId
            );
        } catch (Exception exception) {
            log.error("Could not process stock result message: {}", message, exception);
            throw new IllegalStateException("Could not process stock result", exception);
        }
    }
}
