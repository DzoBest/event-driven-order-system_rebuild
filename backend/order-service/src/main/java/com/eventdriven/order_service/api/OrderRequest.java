package com.eventdriven.order_service.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBalank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record OrderRequest(
    @NotBlank String customerId,
    @NotEmpty List<@Valid ItemRequest> items
) {
    public record ItemRequest(
        @NotBlank String productId,
        @Positive int quantity
    ) {}
}