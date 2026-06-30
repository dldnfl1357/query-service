package com.example.queryservice.config;

import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.concurrent.CompletableFuture;

/**
 * 메시지 처리 실패 시:
 *  - 예외를 다시 throw → SQS ack 안 됨 → visibility timeout 후 재전달
 *  - maxReceiveCount 초과 시 AWS 측에서 DLQ 로 이동 (Terraform redrive_policy 참고)
 * 여기서는 구조화 로깅만 책임지고 재시도 정책 자체는 AWS 인프라가 관리.
 */
@Slf4j
@Configuration
public class SqsErrorHandlerConfig {

    @Bean
    public AsyncErrorHandler<Object> sqsErrorHandler() {
        return (Message<Object> message, Throwable t) -> {
            Object id = message.getHeaders().get("id");
            log.error(
                    "sqs message processing failed messageId={} payloadType={} cause={}",
                    id,
                    message.getPayload() == null ? "null" : message.getPayload().getClass().getSimpleName(),
                    t.getMessage(),
                    t
            );
            return CompletableFuture.failedFuture(t);
        };
    }
}
