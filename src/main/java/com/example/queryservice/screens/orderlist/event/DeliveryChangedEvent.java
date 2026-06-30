package com.example.queryservice.screens.orderlist.event;

import java.time.Instant;

public record DeliveryChangedEvent(
        String eventId,
        Long orderId,
        String deliveryStatus,
        String deliveryAddress,
        Instant deliveryTrackedAt,
        Instant occurredAt
) {
}
