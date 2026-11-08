package com.eventdriven.order_service.processed;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.IdClass;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
@IdClass(ProcessedEventId.class)
public class ProcessedEvent {
    @Id
    private UUID eventId;

    @Id
    private String consumerName;

    private OffsetDateTime processedAt;

    protected ProcessedEvent() {
        // Default constructor for JPA
    }

    public ProcessedEvent(UUID eventId, String consumerName) {
        this.eventId = eventId;
        this.consumerName = consumerName;
        this.processedAt = OffsetDateTime.now();
    }
}