package com.example.queryservice.screens.orderlist.event;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderChangedEvent(
        String eventId,
        Long orderId,
        String orderNumber,
        String orderStatus,
        BigDecimal orderAmount,
        Instant orderedAt,
        Long memberId,
        Long productId,
        Instant occurredAt
) {
}
