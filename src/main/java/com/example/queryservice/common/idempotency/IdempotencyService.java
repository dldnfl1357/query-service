package com.example.queryservice.common.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedEventRepository repository;

    /**
     * 호출 트랜잭션에 합류해 (eventId, consumer) 를 점유.
     * 본 업무 트랜잭션과 같이 커밋되어야 "처리됨" 표시가 의미를 가짐 → REQUIRED.
     *
     * @return true = 최초 점유 (계속 처리), false = 이미 처리된 이벤트 (스킵)
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean tryClaim(String eventId, String consumer) {
        return repository.tryInsert(eventId, consumer, Instant.now()) == 1;
    }
}
