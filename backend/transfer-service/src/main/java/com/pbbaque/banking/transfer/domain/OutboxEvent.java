package com.pbbaque.banking.transfer.domain;

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
import java.util.Objects;
import java.util.UUID;

/** Represents a durable event waiting to be published to the message broker. */
@Entity
@Table(name = "OUTBOX_EVENTS")
public class OutboxEvent {

  @Id
  @Column(name = "ID", nullable = false)
  private UUID id;

  @Column(name = "AGGREGATE_ID", nullable = false)
  private UUID aggregateId;

  @Column(name = "EVENT_TYPE", nullable = false, length = 100)
  private String eventType;

  @Lob
  @Column(name = "PAYLOAD", nullable = false)
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS", nullable = false, length = 20)
  private OutboxEventStatus status;

  @Column(name = "ATTEMPTS", nullable = false)
  private int attempts;

  @Column(name = "LAST_ERROR", length = 1000)
  private String lastError;

  @Column(name = "CREATED_AT", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "AVAILABLE_AT", nullable = false)
  private OffsetDateTime availableAt;

  @Column(name = "PUBLISHED_AT")
  private OffsetDateTime publishedAt;

  protected OutboxEvent() {}

  private OutboxEvent(
      UUID id,
      UUID aggregateId,
      String eventType,
      String payload,
      OutboxEventStatus status,
      int attempts,
      String lastError,
      OffsetDateTime createdAt,
      OffsetDateTime availableAt,
      OffsetDateTime publishedAt) {
    this.id = id;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.payload = payload;
    this.status = status;
    this.attempts = attempts;
    this.lastError = lastError;
    this.createdAt = createdAt;
    this.availableAt = availableAt;
    this.publishedAt = publishedAt;
  }

  /**
   * Creates a new pending outbox event ready for publication.
   *
   * @param id unique event identifier
   * @param aggregateId identifier of the aggregate that produced the event
   * @param eventType logical event type
   * @param payload serialized event payload
   * @param occurredAt time at which the event occurred
   * @return new pending outbox event
   */
  public static OutboxEvent create(
      UUID id, UUID aggregateId, String eventType, String payload, OffsetDateTime occurredAt) {

    UUID normalizedId = Objects.requireNonNull(id, "id must not be null");
    UUID normalizedAggregateId =
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
    String normalizedEventType = requireNonBlank(eventType, "eventType");
    String normalizedPayload = requireNonBlank(payload, "payload");
    OffsetDateTime normalizedOccurredAt =
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");

    return new OutboxEvent(
        normalizedId,
        normalizedAggregateId,
        normalizedEventType,
        normalizedPayload,
        OutboxEventStatus.PENDING,
        0,
        null,
        normalizedOccurredAt,
        normalizedOccurredAt,
        null);
  }

  /**
   * Marks this event as successfully published.
   *
   * @param publishedAt publication timestamp
   */
  public void markPublished(OffsetDateTime publishedAt) {
    this.status = OutboxEventStatus.PUBLISHED;
    this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt must not be null");
    this.lastError = null;
  }

  /**
   * Records a failed publication attempt and schedules the next attempt.
   *
   * @param error description of the publication error
   * @param nextAvailableAt timestamp from which the event may be retried
   */
  public void registerPublicationFailure(String error, OffsetDateTime nextAvailableAt) {
    this.attempts++;
    this.lastError = normalizeError(error);
    this.availableAt = Objects.requireNonNull(nextAvailableAt, "nextAvailableAt must not be null");
  }

  @PrePersist
  void prePersist() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    if (id == null) {
      id = UUID.randomUUID();
    }

    if (status == null) {
      status = OutboxEventStatus.PENDING;
    }

    if (createdAt == null) {
      createdAt = now;
    }

    if (availableAt == null) {
      availableAt = createdAt;
    }
  }

  private static String requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }

    return value.trim();
  }

  private static String normalizeError(String error) {
    if (error == null || error.isBlank()) {
      return null;
    }

    String normalized = error.trim();

    if (normalized.length() <= 1000) {
      return normalized;
    }

    return normalized.substring(0, 1000);
  }

  public UUID getId() {
    return id;
  }

  public UUID getAggregateId() {
    return aggregateId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getPayload() {
    return payload;
  }

  public OutboxEventStatus getStatus() {
    return status;
  }

  public int getAttempts() {
    return attempts;
  }

  public String getLastError() {
    return lastError;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getAvailableAt() {
    return availableAt;
  }

  public OffsetDateTime getPublishedAt() {
    return publishedAt;
  }
}
