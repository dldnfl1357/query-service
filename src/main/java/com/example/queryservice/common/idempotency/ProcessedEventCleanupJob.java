package com.example.queryservice.common.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessedEventCleanupJob {

    private final ProcessedEventRepository repository;

    @Value("${query-service.idempotency.retention:P7D}")
    private Duration retention;

    @Scheduled(cron = "${query-service.idempotency.cleanup-cron:0 0 4 * * *}")
    @Transactional
    public void cleanup() {
        Instant threshold = Instant.now().minus(retention);
        int deleted = repository.deleteOlderThan(threshold);
        log.info("processed_event cleanup deleted={} threshold={}", deleted, threshold);
    }
}
