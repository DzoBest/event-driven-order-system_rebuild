package com.eventdriven.order_service.order;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(UUID id) {
        super("Order with ID " + id + " not found.");
    }
}