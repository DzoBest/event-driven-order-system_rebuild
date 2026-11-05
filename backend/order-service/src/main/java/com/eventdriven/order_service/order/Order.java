package com.eventdriven.order_service.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    private UUID id;
    private String customerId;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
        // Default constructor for JPA
    }

    public Order(UUID id, String customerId, List<OrderItem> items) {
        this.id = id;
        this.customerId = customerId;
        this.status = OrderStatus.PENDING;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
        items.forEach(this::addItem);
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public void updateStatus(OrderStatus newStatus) {
        if(status != OrderStatus.PENDING) {
            throw new IllegalStateException("Order status can only be updated from PENDING state.");
        }
        status = newStatus;
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }
    public String getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }
}
