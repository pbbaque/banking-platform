package com.pbbaque.banking.transfer.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties for the transactional outbox publisher. */
@ConfigurationProperties(prefix = "messaging.kafka.outbox")
public record OutboxPublisherProperties(
    int batchSize, Duration sendTimeout, Duration retryDelay, Duration pollingInterval) {}
