package com.eventdriven.order_service.api;

import com.eventdriven.order_service.order.Order;
import com.eventdriven.order_service.order.OrderStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerId,
        List<ItemResponse> items,
        OrderStatus status,
        OffsetDateTime createdAt
) {
    public static OrderResponse from(Order order){
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getItems().stream()
                        .map(item -> new ItemResponse(item.getProductId(), item.getQuantity()))
                        .toList(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }

    public record ItemResponse(
            String productId,
            int quantity
    ) {}

}