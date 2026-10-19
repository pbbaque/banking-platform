package com.pbbaque.banking.audit.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbbaque.banking.audit.dto.AuditEventMessage;
import com.pbbaque.banking.audit.service.AuditEventService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Kafka consumer responsible for persisting incoming audit events. */
@Component
public class AuditEventKafkaListener {

  private final AuditEventService auditEventService;
  private final ObjectMapper objectMapper;

  /**
   * Creates the audit event Kafka listener.
   *
   * @param auditEventService audit event application service
   * @param objectMapper JSON deserializer
   */
  public AuditEventKafkaListener(AuditEventService auditEventService, ObjectMapper objectMapper) {
    this.auditEventService = auditEventService;
    this.objectMapper = objectMapper.copy();
  }

  /**
   * Consumes and persists an audit event.
   *
   * @param payload serialized audit event message
   */
  @KafkaListener(
      topics = "${messaging.kafka.topics.audit-events.name}",
      groupId = "${spring.kafka.consumer.group-id}")
  public void consume(String payload) {
    AuditEventMessage message = deserialize(payload);

    auditEventService.createFromMessage(message);
  }

  private AuditEventMessage deserialize(String payload) {
    try {
      return objectMapper.readValue(payload, AuditEventMessage.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Unable to deserialize audit event message", exception);
    }
  }
}
