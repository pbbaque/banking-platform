package com.pbbaque.banking.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** Represents an immutable audit event recorded by the banking platform. */
@Entity
@Table(name = "AUDIT_EVENTS")
public class AuditEvent {

  @Id
  @Column(name = "ID", nullable = false)
  private UUID id;

  @Column(name = "EVENT_TYPE", nullable = false, length = 100)
  private String eventType;

  @Column(name = "AGGREGATE_TYPE", nullable = false, length = 100)
  private String aggregateType;

  @Column(name = "AGGREGATE_ID", nullable = false, length = 100)
  private String aggregateId;

  @Column(name = "ACTOR_ID")
  private UUID actorId;

  @Enumerated(EnumType.STRING)
  @Column(name = "OUTCOME", nullable = false, length = 20)
  private AuditOutcome outcome;

  @Column(name = "DESCRIPTION", length = 500)
  private String description;

  @Lob
  @Column(name = "METADATA")
  private String metadata;

  @Column(name = "OCCURRED_AT", nullable = false)
  private OffsetDateTime occurredAt;

  @Column(name = "CREATED_AT", nullable = false)
  private OffsetDateTime createdAt;

  protected AuditEvent() {}

  private AuditEvent(
      UUID id,
      String eventType,
      String aggregateType,
      String aggregateId,
      UUID actorId,
      AuditOutcome outcome,
      String description,
      String metadata,
      OffsetDateTime occurredAt) {
    this.id = id;
    this.eventType = eventType;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.actorId = actorId;
    this.outcome = outcome;
    this.description = description;
    this.metadata = metadata;
    this.occurredAt = occurredAt;
  }

  /**
   * Creates a new audit event.
   *
   * @param eventType type of event being recorded
   * @param aggregateType type of aggregate affected by the event
   * @param aggregateId identifier of the affected aggregate
   * @param actorId identifier of the actor responsible for the action, if available
   * @param outcome result of the audited operation
   * @param description optional human-readable description
   * @param metadata optional structured metadata serialized as text
   * @param occurredAt instant when the audited event occurred
   * @return a new audit event
   */
  public static AuditEvent create(
      String eventType,
      String aggregateType,
      String aggregateId,
      UUID actorId,
      AuditOutcome outcome,
      String description,
      String metadata,
      OffsetDateTime occurredAt) {

    String normalizedEventType = normalizeRequired(eventType, "eventType");
    String normalizedAggregateType = normalizeRequired(aggregateType, "aggregateType");
    String normalizedAggregateId = normalizeRequired(aggregateId, "aggregateId");

    AuditOutcome validatedOutcome = requireOutcome(outcome);
    OffsetDateTime validatedOccurredAt = requireOccurredAt(occurredAt);

    String normalizedDescription = normalizeOptional(description);
    String normalizedMetadata = normalizeOptional(metadata);

    return new AuditEvent(
        UUID.randomUUID(),
        normalizedEventType,
        normalizedAggregateType,
        normalizedAggregateId,
        actorId,
        validatedOutcome,
        normalizedDescription,
        normalizedMetadata,
        validatedOccurredAt);
  }

  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }

    if (createdAt == null) {
      createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
  }

  private static String normalizeRequired(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }

    return value.trim();
  }

  private static String normalizeOptional(String value) {
    if (value == null) {
      return null;
    }

    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private static AuditOutcome requireOutcome(AuditOutcome outcome) {
    if (outcome == null) {
      throw new IllegalArgumentException("outcome must not be null");
    }

    return outcome;
  }

  private static OffsetDateTime requireOccurredAt(OffsetDateTime occurredAt) {
    if (occurredAt == null) {
      throw new IllegalArgumentException("occurredAt must not be null");
    }

    return occurredAt;
  }

  public UUID getId() {
    return id;
  }

  public String getEventType() {
    return eventType;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public String getAggregateId() {
    return aggregateId;
  }

  public UUID getActorId() {
    return actorId;
  }

  public AuditOutcome getOutcome() {
    return outcome;
  }

  public String getDescription() {
    return description;
  }

  public String getMetadata() {
    return metadata;
  }

  public OffsetDateTime getOccurredAt() {
    return occurredAt;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
