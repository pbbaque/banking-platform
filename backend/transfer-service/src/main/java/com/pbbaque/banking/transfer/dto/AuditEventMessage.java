package com.pbbaque.banking.transfer.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/** Message representing an auditable event produced by the transfer service. */
public record AuditEventMessage(
    UUID eventId,
    String eventType,
    String aggregateType,
    String aggregateId,
    UUID actorId,
    String outcome,
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
