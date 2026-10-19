package com.pbbaque.banking.transfer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Kafka configuration for the transfer service. */
@Configuration
@EnableConfigurationProperties({AuditEventsTopicProperties.class, OutboxPublisherProperties.class})
public class KafkaTopicConfig {

  /**
   * Declares the Kafka topic used for audit events.
   *
   * @param properties audit events topic configuration
   * @return audit events Kafka topic definition
   */
  @Bean
  public NewTopic auditEventsTopic(AuditEventsTopicProperties properties) {
    return TopicBuilder.name(properties.name())
        .partitions(properties.partitions())
        .replicas(properties.replicationFactor())
        .build();
  }
}
