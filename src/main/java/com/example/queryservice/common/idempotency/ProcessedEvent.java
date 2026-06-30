package com.example.queryservice.common.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * (eventId, consumer) 단위로 처리 이력을 남겨 중복 처리를 차단.
 * consumer = 화면(read-model) 이름. 같은 도메인 이벤트를 여러 화면이 구독해도 독립적으로 멱등성 보장.
 */
@Entity
@Table(
        name = "processed_event",
        indexes = @Index(name = "idx_processed_at", columnList = "processed_at")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {

    @EmbeddedId
    private Key id;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public ProcessedEvent(String eventId, String consumer, Instant processedAt) {
        this.id = new Key(eventId, consumer);
        this.processedAt = processedAt;
    }

    @Embeddable
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Key implements Serializable {

        @Column(name = "event_id", length = 64, nullable = false)
        private String eventId;

        @Column(name = "consumer", length = 64, nullable = false)
        private String consumer;

        public Key(String eventId, String consumer) {
            this.eventId = eventId;
            this.consumer = consumer;
        }
    }
}
