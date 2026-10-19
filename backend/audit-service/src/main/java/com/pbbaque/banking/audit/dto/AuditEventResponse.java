package com.pbbaque.banking.audit.dto;

import com.pbbaque.banking.audit.domain.AuditEvent;
import com.pbbaque.banking.audit.domain.AuditOutcome;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Response representation of an audit event. */
public record AuditEventResponse(
    UUID id,
    String eventType,
    String aggregateType,
    String aggregateId,
    UUID actorId,
    AuditOutcome outcome,
    String description,
    String metadata,
    OffsetDateTime occurredAt,
    OffsetDateTime createdAt) {

  /**
   * Creates a response from an audit event.
   *
   * @param auditEvent audit event
   * @return response representation
   */
  public static AuditEventResponse from(AuditEvent auditEvent) {
    return new AuditEventResponse(
        auditEvent.getId(),
        auditEvent.getEventType(),
        auditEvent.getAggregateType(),
        auditEvent.getAggregateId(),
        auditEvent.getActorId(),
        auditEvent.getOutcome(),
        auditEvent.getDescription(),
        auditEvent.getMetadata(),
        auditEvent.getOccurredAt(),
        auditEvent.getCreatedAt());
  }
}
