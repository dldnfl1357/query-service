package com.example.queryservice.screens.orderlist.event;

import java.time.Instant;

public record MemberChangedEvent(
        String eventId,
        Long memberId,
        String memberName,
        String memberEmail,
        String memberGrade,
        Instant occurredAt
) {
}
