package com.eventdriven.order_service.outbox;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    private UUID id;
    private UUID aggregateId;
    private String eventType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;
    private OffsetDateTime createdAt;
    private OffsetDateTime publishedAt;
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    protected OutboxEvent() {
        // Default constructor for JPA
    }

    public OutboxEvent(UUID id, UUID aggregateId, String eventType, String payload) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = OffsetDateTime.now();
        this.status = OutboxStatus.PENDING;
    }

    public void markAsPublished() {
        status = OutboxStatus.PUBLISHED;
        publishedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }
    public UUID getAggregateId() {
        return aggregateId;
    }
    public String getEventType() {
        return eventType;
    }
    public String getPayload() {
        return payload;
    }
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }
    public OutboxStatus getStatus() {
        return status;
    }
}