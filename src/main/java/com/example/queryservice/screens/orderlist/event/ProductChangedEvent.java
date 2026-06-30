package com.example.queryservice.screens.orderlist.event;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductChangedEvent(
        String eventId,
        Long productId,
        String productName,
        String productCategory,
        BigDecimal productPrice,
        Instant occurredAt
) {
}
