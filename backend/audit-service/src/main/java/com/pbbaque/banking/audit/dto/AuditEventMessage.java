package com.pbbaque.banking.audit.dto;

import com.pbbaque.banking.audit.domain.AuditOutcome;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/** Message consumed from Kafka to create an audit event. */
public record AuditEventMessage(
    UUID eventId,
    String eventType,
    String aggregateType,
    String aggregateId,
    UUID actorId,
    AuditOutcome outcome,
    String description,
    Map<String, Object> metadata,
    OffsetDateTime occurredAt) {

  /** Creates an immutable audit event message. */
  public AuditEventMessage {
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
  }

  /**
   * Returns an immutable copy of the event metadata.
   *
   * @return event metadata
   */
  @Override
  public Map<String, Object> metadata() {
    return Map.copyOf(metadata);
  }
}
