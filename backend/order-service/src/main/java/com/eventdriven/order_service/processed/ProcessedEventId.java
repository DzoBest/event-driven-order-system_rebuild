package com.eventdriven.order_service.processed;

import java.io.Serializable;
import java.util.UUID;

public class ProcessedEventId implements Serializable {
    private UUID eventId;
    private String consumerName;

    public ProcessedEventId() {
    }

    public ProcessedEventId(UUID eventId, String consumerName) {
        this.eventId = eventId;
        this.consumerName = consumerName;
    }

    @Override
    public boolean equals(Object other) {
        if ( !(other instanceof ProcessedEventId id) ) {
            return false;
        }
        
        return eventId.equals(id.eventId) && consumerName.equals(id.consumerName);
    }

    @Override
    public int hashCode() {
        return 31 * eventId.hashCode() + consumerName.hashCode();
    }
}