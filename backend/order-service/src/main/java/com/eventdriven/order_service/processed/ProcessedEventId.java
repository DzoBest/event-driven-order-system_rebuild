package com.eventdriven.order_service.processed;

import java.io.Serializable;
import java.util.UUID;

public class ProcessedEventId implements Serializable {
    private UUID eventId;
    private String eventType;

    public ProcessedEventId() {
    }

    public ProcessedEventId(UUID eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
    }

    @Override
    public boolean equals(Object other) {
        if ( !(other instanceof ProcessedEventId id) ) {
            return false;
        }
        
        return eventId.equals(id.eventId) && ConsumerName.equals(id.ConsumerName);
    }

    @Override
    public int hashCode() {
        return 31 * eventId.hashCode() + ConsumerName.hashCode();
    }
}