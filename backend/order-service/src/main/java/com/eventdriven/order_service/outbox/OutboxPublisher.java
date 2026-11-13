package com.eventdriven.order_service.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository repository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish.fixed-delay-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        for(OutboxEvent event : repository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)) {
            try {
                kafkaTemplate.send("orders.created", event.getAggregateId().toString(), event.getPayload()).get();
                event.markAsPublished();
                repository.save(event);
                log.info("Published outbox event with ID: {}", event.getId());
            } catch (Exception e) {
                log.warn("Could not publish outbox event with ID: {}", event.getId(), e);
                return; // Exit the loop to retry in the next scheduled run
            }
        }
    }
}