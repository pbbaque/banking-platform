package com.pbbaque.banking.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties for the Kafka audit events topic. */
@ConfigurationProperties(prefix = "messaging.kafka.topics.audit-events")
public record AuditEventsTopicProperties(String name) {}
