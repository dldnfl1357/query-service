package com.example.queryservice.common.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEvent.Key> {

    @Modifying
    @Query(
            value = "insert ignore into processed_event (event_id, consumer, processed_at) values (:eventId, :consumer, :processedAt)",
            nativeQuery = true
    )
    int tryInsert(
            @Param("eventId") String eventId,
            @Param("consumer") String consumer,
            @Param("processedAt") Instant processedAt
    );

    @Modifying
    @Query("delete from ProcessedEvent p where p.processedAt < :threshold")
    int deleteOlderThan(@Param("threshold") Instant threshold);
}
