package com.pbbaque.banking.transfer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties for the audit events Kafka topic. */
@ConfigurationProperties(prefix = "messaging.kafka.topics.audit-events")
public record AuditEventsTopicProperties(String name, int partitions, short replicationFactor) {}
