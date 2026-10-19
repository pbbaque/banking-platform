package com.pbbaque.banking.audit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AuditEvent}. */
class AuditEventTest {

  @Test
  void shouldCreateAuditEvent() {
    UUID actorId = UUID.randomUUID();
    OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);

    AuditEvent auditEvent =
        AuditEvent.create(
            "TRANSFER_COMPLETED",
            "TRANSFER",
            "transfer-123",
            actorId,
            AuditOutcome.SUCCESS,
            "Transfer completed successfully",
            "{\"amount\":100}",
            occurredAt);

    assertNotNull(auditEvent.getId());
    assertEquals("TRANSFER_COMPLETED", auditEvent.getEventType());
    assertEquals("TRANSFER", auditEvent.getAggregateType());
    assertEquals("transfer-123", auditEvent.getAggregateId());
    assertEquals(actorId, auditEvent.getActorId());
    assertEquals(AuditOutcome.SUCCESS, auditEvent.getOutcome());
    assertEquals("Transfer completed successfully", auditEvent.getDescription());
    assertEquals("{\"amount\":100}", auditEvent.getMetadata());
    assertEquals(occurredAt, auditEvent.getOccurredAt());
    assertNull(auditEvent.getCreatedAt());
  }

  @Test
  void shouldCreateAuditEventWithoutOptionalValues() {
    OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);

    AuditEvent auditEvent =
        AuditEvent.create(
            "ACCOUNT_CREATED",
            "ACCOUNT",
            UUID.randomUUID().toString(),
            null,
            AuditOutcome.SUCCESS,
            null,
            null,
            occurredAt);

    assertNull(auditEvent.getActorId());
    assertNull(auditEvent.getDescription());
    assertNull(auditEvent.getMetadata());
  }

  @Test
  void shouldNormalizeRequiredFields() {
    AuditEvent auditEvent =
        AuditEvent.create(
            "  TRANSFER_FAILED  ",
            "  TRANSFER  ",
            "  transfer-123  ",
            null,
            AuditOutcome.FAILURE,
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    assertEquals("TRANSFER_FAILED", auditEvent.getEventType());
    assertEquals("TRANSFER", auditEvent.getAggregateType());
    assertEquals("transfer-123", auditEvent.getAggregateId());
  }

  @Test
  void shouldNormalizeOptionalFields() {
    AuditEvent auditEvent =
        AuditEvent.create(
            "TRANSFER_FAILED",
            "TRANSFER",
            "transfer-123",
            null,
            AuditOutcome.FAILURE,
            "  Destination credit failed  ",
            "  {\"reason\":\"ACCOUNT_NOT_FOUND\"}  ",
            OffsetDateTime.now(ZoneOffset.UTC));

    assertEquals("Destination credit failed", auditEvent.getDescription());
    assertEquals("{\"reason\":\"ACCOUNT_NOT_FOUND\"}", auditEvent.getMetadata());
  }

  @Test
  void shouldConvertBlankOptionalFieldsToNull() {
    AuditEvent auditEvent =
        AuditEvent.create(
            "TRANSFER_FAILED",
            "TRANSFER",
            "transfer-123",
            null,
            AuditOutcome.FAILURE,
            "   ",
            "   ",
            OffsetDateTime.now(ZoneOffset.UTC));

    assertNull(auditEvent.getDescription());
    assertNull(auditEvent.getMetadata());
  }

  @Test
  void shouldRejectNullEventType() {
    OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                AuditEvent.create(
                    null,
                    "TRANSFER",
                    "transfer-123",
                    null,
                    AuditOutcome.SUCCESS,
                    null,
                    null,
                    occurredAt));

    assertEquals("eventType must not be blank", exception.getMessage());
  }

  @Test
  void shouldRejectBlankEventType() {
    OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                AuditEvent.create(
                    "   ",
                    "TRANSFER",
                    "transfer-123",
                    null,
                    AuditOutcome.SUCCESS,
                    null,
                    null,
                    occurredAt));

    assertEquals("eventType must not be blank", exception.getMessage());
  }

  @Test
  void shouldRejectBlankAggregateType() {
    OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                AuditEvent.create(
                    "TRANSFER_COMPLETED",
                    "   ",
                    "transfer-123",
                    null,
                    AuditOutcome.SUCCESS,
                    null,
                    null,
                    occurredAt));

    assertEquals("aggregateType must not be blank", exception.getMessage());
  }

  @Test
  void shouldRejectBlankAggregateId() {
    OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                AuditEvent.create(
                    "TRANSFER_COMPLETED",
                    "TRANSFER",
                    "   ",
                    null,
                    AuditOutcome.SUCCESS,
                    null,
                    null,
                    occurredAt));

    assertEquals("aggregateId must not be blank", exception.getMessage());
  }

  @Test
  void shouldRejectNullOutcome() {
    OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                AuditEvent.create(
                    "TRANSFER_COMPLETED",
                    "TRANSFER",
                    "transfer-123",
                    null,
                    null,
                    null,
                    null,
                    occurredAt));

    assertEquals("outcome must not be null", exception.getMessage());
  }

  @Test
  void shouldRejectNullOccurredAt() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                AuditEvent.create(
                    "TRANSFER_COMPLETED",
                    "TRANSFER",
                    "transfer-123",
                    null,
                    AuditOutcome.SUCCESS,
                    null,
                    null,
                    null));

    assertEquals("occurredAt must not be null", exception.getMessage());
  }

  @Test
  void shouldSetCreatedAtBeforePersistence() {
    AuditEvent auditEvent =
        AuditEvent.create(
            "ACCOUNT_CREATED",
            "ACCOUNT",
            UUID.randomUUID().toString(),
            null,
            AuditOutcome.SUCCESS,
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    assertNull(auditEvent.getCreatedAt());

    auditEvent.prePersist();

    assertNotNull(auditEvent.getCreatedAt());
    assertEquals(ZoneOffset.UTC, auditEvent.getCreatedAt().getOffset());
  }
}
