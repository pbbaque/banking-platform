package com.pbbaque.banking.transfer.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link OutboxEvent}. */
class OutboxEventTest {

  private static final UUID EVENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

  private static final UUID AGGREGATE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

  private static final OffsetDateTime OCCURRED_AT =
      OffsetDateTime.of(2026, 10, 19, 1, 30, 0, 0, ZoneOffset.UTC);

  @Test
  void shouldCreatePendingOutboxEvent() {
    OutboxEvent event =
        OutboxEvent.create(
            EVENT_ID,
            AGGREGATE_ID,
            "TRANSFER_COMPLETED",
            "{\"status\":\"COMPLETED\"}",
            OCCURRED_AT);

    assertEquals(EVENT_ID, event.getId());
    assertEquals(AGGREGATE_ID, event.getAggregateId());
    assertEquals("TRANSFER_COMPLETED", event.getEventType());
    assertEquals("{\"status\":\"COMPLETED\"}", event.getPayload());
    assertEquals(OutboxEventStatus.PENDING, event.getStatus());
    assertEquals(0, event.getAttempts());
    assertNull(event.getLastError());
    assertEquals(OCCURRED_AT, event.getCreatedAt());
    assertEquals(OCCURRED_AT, event.getAvailableAt());
    assertNull(event.getPublishedAt());
  }

  @Test
  void shouldNormalizeEventTypeAndPayload() {
    OutboxEvent event =
        OutboxEvent.create(
            EVENT_ID,
            AGGREGATE_ID,
            "  TRANSFER_COMPLETED  ",
            "  {\"status\":\"COMPLETED\"}  ",
            OCCURRED_AT);

    assertEquals("TRANSFER_COMPLETED", event.getEventType());
    assertEquals("{\"status\":\"COMPLETED\"}", event.getPayload());
  }

  @Test
  void shouldRejectNullEventId() {
    assertThrows(
        NullPointerException.class,
        () ->
            OutboxEvent.create(
                null,
                AGGREGATE_ID,
                "TRANSFER_COMPLETED",
                "{\"status\":\"COMPLETED\"}",
                OCCURRED_AT));
  }

  @Test
  void shouldRejectNullAggregateId() {
    assertThrows(
        NullPointerException.class,
        () ->
            OutboxEvent.create(
                EVENT_ID, null, "TRANSFER_COMPLETED", "{\"status\":\"COMPLETED\"}", OCCURRED_AT));
  }

  @Test
  void shouldRejectBlankEventType() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            OutboxEvent.create(
                EVENT_ID, AGGREGATE_ID, "   ", "{\"status\":\"COMPLETED\"}", OCCURRED_AT));
  }

  @Test
  void shouldRejectBlankPayload() {
    assertThrows(
        IllegalArgumentException.class,
        () -> OutboxEvent.create(EVENT_ID, AGGREGATE_ID, "TRANSFER_COMPLETED", "   ", OCCURRED_AT));
  }

  @Test
  void shouldRejectNullOccurredAt() {
    assertThrows(
        NullPointerException.class,
        () ->
            OutboxEvent.create(
                EVENT_ID, AGGREGATE_ID, "TRANSFER_COMPLETED", "{\"status\":\"COMPLETED\"}", null));
  }

  @Test
  void shouldMarkEventAsPublished() {
    OutboxEvent event = createEvent();

    OffsetDateTime publishedAt = OCCURRED_AT.plusSeconds(5);

    event.markPublished(publishedAt);

    assertEquals(OutboxEventStatus.PUBLISHED, event.getStatus());
    assertEquals(publishedAt, event.getPublishedAt());
    assertNull(event.getLastError());
  }

  @Test
  void shouldRejectNullPublishedAt() {
    OutboxEvent event = createEvent();

    assertThrows(NullPointerException.class, () -> event.markPublished(null));
  }

  @Test
  void shouldRegisterPublicationFailure() {
    OutboxEvent event = createEvent();

    OffsetDateTime nextAttempt = OCCURRED_AT.plusSeconds(30);

    event.registerPublicationFailure("Kafka unavailable", nextAttempt);

    assertEquals(OutboxEventStatus.PENDING, event.getStatus());
    assertEquals(1, event.getAttempts());
    assertEquals("Kafka unavailable", event.getLastError());
    assertEquals(nextAttempt, event.getAvailableAt());
    assertNull(event.getPublishedAt());
  }

  @Test
  void shouldIncrementPublicationAttempts() {
    OutboxEvent event = createEvent();

    event.registerPublicationFailure("First failure", OCCURRED_AT.plusSeconds(10));
    event.registerPublicationFailure("Second failure", OCCURRED_AT.plusSeconds(30));

    assertEquals(2, event.getAttempts());
    assertEquals("Second failure", event.getLastError());
    assertEquals(OCCURRED_AT.plusSeconds(30), event.getAvailableAt());
  }

  @Test
  void shouldNormalizeBlankPublicationErrorToNull() {
    OutboxEvent event = createEvent();

    event.registerPublicationFailure("   ", OCCURRED_AT.plusSeconds(10));

    assertEquals(1, event.getAttempts());
    assertNull(event.getLastError());
  }

  @Test
  void shouldTruncatePublicationErrorToDatabaseLimit() {
    OutboxEvent event = createEvent();

    String error = "x".repeat(1200);

    event.registerPublicationFailure(error, OCCURRED_AT.plusSeconds(10));

    assertNotNull(event.getLastError());
    assertEquals(1000, event.getLastError().length());
  }

  @Test
  void shouldRejectNullNextAvailableAt() {
    OutboxEvent event = createEvent();

    assertThrows(
        NullPointerException.class,
        () -> event.registerPublicationFailure("Kafka unavailable", null));
  }

  private OutboxEvent createEvent() {
    return OutboxEvent.create(
        EVENT_ID, AGGREGATE_ID, "TRANSFER_COMPLETED", "{\"status\":\"COMPLETED\"}", OCCURRED_AT);
  }
}
