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

/** Represents an immutable audit event persisted by the audit service. */
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
      OffsetDateTime occurredAt,
      OffsetDateTime createdAt) {
    this.id = id;
    this.eventType = eventType;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.actorId = actorId;
    this.outcome = outcome;
    this.description = description;
    this.metadata = metadata;
    this.occurredAt = occurredAt;
    this.createdAt = createdAt;
  }

  /**
   * Creates a new audit event with an internally generated identifier.
   *
   * @param eventType event type
   * @param aggregateType aggregate type
   * @param aggregateId aggregate identifier
   * @param actorId actor identifier
   * @param outcome event outcome
   * @param description optional event description
   * @param metadata optional serialized metadata
   * @param occurredAt event occurrence timestamp
   * @return new audit event
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
    return create(
        UUID.randomUUID(),
        eventType,
        aggregateType,
        aggregateId,
        actorId,
        outcome,
        description,
        metadata,
        occurredAt);
  }

  /**
   * Creates a new audit event with a supplied identifier.
   *
   * <p>This factory is intended for externally produced events whose identifier must be preserved
   * for idempotent processing.
   *
   * @param id event identifier
   * @param eventType event type
   * @param aggregateType aggregate type
   * @param aggregateId aggregate identifier
   * @param actorId actor identifier
   * @param outcome event outcome
   * @param description optional event description
   * @param metadata optional serialized metadata
   * @param occurredAt event occurrence timestamp
   * @return new audit event
   */
  public static AuditEvent create(
      UUID id,
      String eventType,
      String aggregateType,
      String aggregateId,
      UUID actorId,
      AuditOutcome outcome,
      String description,
      String metadata,
      OffsetDateTime occurredAt) {

    UUID normalizedId = requireNonNull(id, "id");
    String normalizedEventType = requireNonBlank(eventType, "eventType");
    String normalizedAggregateType = requireNonBlank(aggregateType, "aggregateType");
    String normalizedAggregateId = requireNonBlank(aggregateId, "aggregateId");
    AuditOutcome normalizedOutcome = requireNonNull(outcome, "outcome");
    String normalizedDescription = normalizeOptional(description);
    String normalizedMetadata = normalizeOptional(metadata);
    OffsetDateTime normalizedOccurredAt = requireNonNull(occurredAt, "occurredAt");

    return new AuditEvent(
        normalizedId,
        normalizedEventType,
        normalizedAggregateType,
        normalizedAggregateId,
        actorId,
        normalizedOutcome,
        normalizedDescription,
        normalizedMetadata,
        normalizedOccurredAt,
        null);
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

  private static String requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }

    return value.trim();
  }

  private static <T> T requireNonNull(T value, String fieldName) {
    if (value == null) {
      throw new IllegalArgumentException(fieldName + " must not be null");
    }

    return value;
  }

  private static String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    return value.trim();
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
