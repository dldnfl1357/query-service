package com.example.queryservice.config;

import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * spring.threads.virtual.enabled=true 만으로
 *  - Tomcat 요청 스레드
 *  - @Async / 기본 taskExecutor
 *  - spring-cloud-aws SQS 리스너 (3.2.x+)
 * 모두 가상 스레드를 사용합니다. 이 빈은 명시적 사용처를 위한 백업입니다.
 */
@Configuration
public class VirtualThreadConfig {

    @Bean
    public AsyncTaskExecutor applicationTaskExecutor(SimpleAsyncTaskExecutorBuilder builder) {
        return builder.virtualThreads(true).build();
    }
}
