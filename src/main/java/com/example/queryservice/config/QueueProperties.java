package com.example.queryservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "query-service.queues")
public record QueueProperties(
        String order,
        String member,
        String product,
        String delivery
) {
}
