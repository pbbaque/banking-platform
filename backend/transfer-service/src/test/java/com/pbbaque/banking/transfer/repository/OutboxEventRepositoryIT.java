package com.pbbaque.banking.transfer.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pbbaque.banking.transfer.domain.OutboxEvent;
import com.pbbaque.banking.transfer.domain.OutboxEventStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

/** Integration tests for {@link OutboxEventRepository}. */
@DataJpaTest
@ActiveProfiles("local")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OutboxEventRepositoryIT {

  private static final UUID EVENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

  private static final UUID SECOND_EVENT_ID =
      UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

  private static final UUID AGGREGATE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

  private static final OffsetDateTime BASE_TIME =
      OffsetDateTime.of(2026, 10, 19, 1, 30, 0, 0, ZoneOffset.UTC);

  @Autowired private OutboxEventRepository outboxEventRepository;

  @Test
  void shouldPersistAndLoadOutboxEvent() {
    OutboxEvent event = createEvent(EVENT_ID, BASE_TIME);

    outboxEventRepository.saveAndFlush(event);

    OutboxEvent persisted = outboxEventRepository.findById(EVENT_ID).orElseThrow();

    assertEquals(EVENT_ID, persisted.getId());
    assertEquals(AGGREGATE_ID, persisted.getAggregateId());
    assertEquals("TRANSFER_COMPLETED", persisted.getEventType());
    assertEquals("{\"status\":\"COMPLETED\"}", persisted.getPayload());
    assertEquals(OutboxEventStatus.PENDING, persisted.getStatus());
    assertEquals(0, persisted.getAttempts());
    assertEquals(BASE_TIME, persisted.getCreatedAt());
    assertEquals(BASE_TIME, persisted.getAvailableAt());
    assertNotNull(persisted.getPayload());
  }

  @Test
  void shouldFindPublishablePendingEvents() {
    OutboxEvent availableEvent = createEvent(EVENT_ID, BASE_TIME);

    OutboxEvent futureEvent = createEvent(SECOND_EVENT_ID, BASE_TIME.plusMinutes(10));

    outboxEventRepository.save(availableEvent);
    outboxEventRepository.save(futureEvent);
    outboxEventRepository.flush();

    List<OutboxEvent> result =
        outboxEventRepository.findByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            OutboxEventStatus.PENDING, BASE_TIME.plusMinutes(1), PageRequest.of(0, 10));

    assertEquals(1, result.size());
    assertEquals(EVENT_ID, result.get(0).getId());
  }

  @Test
  void shouldOrderPublishableEventsByCreationTime() {
    OutboxEvent second = createEvent(SECOND_EVENT_ID, BASE_TIME.plusSeconds(10));

    OutboxEvent first = createEvent(EVENT_ID, BASE_TIME);

    outboxEventRepository.save(second);
    outboxEventRepository.save(first);
    outboxEventRepository.flush();

    List<OutboxEvent> result =
        outboxEventRepository.findByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            OutboxEventStatus.PENDING, BASE_TIME.plusMinutes(1), PageRequest.of(0, 10));

    assertEquals(2, result.size());
    assertEquals(EVENT_ID, result.get(0).getId());
    assertEquals(SECOND_EVENT_ID, result.get(1).getId());
  }

  @Test
  void shouldRespectPageSizeWhenFindingPendingEvents() {
    OutboxEvent first = createEvent(EVENT_ID, BASE_TIME);

    OutboxEvent second = createEvent(SECOND_EVENT_ID, BASE_TIME.plusSeconds(1));

    outboxEventRepository.save(first);
    outboxEventRepository.save(second);
    outboxEventRepository.flush();

    List<OutboxEvent> result =
        outboxEventRepository.findByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            OutboxEventStatus.PENDING, BASE_TIME.plusMinutes(1), PageRequest.of(0, 1));

    assertEquals(1, result.size());
    assertEquals(EVENT_ID, result.get(0).getId());
  }

  @Test
  void shouldPersistPublicationFailureState() {
    OutboxEvent event = createEvent(EVENT_ID, BASE_TIME);

    event.registerPublicationFailure("Kafka unavailable", BASE_TIME.plusSeconds(30));

    outboxEventRepository.saveAndFlush(event);

    OutboxEvent persisted = outboxEventRepository.findById(EVENT_ID).orElseThrow();

    assertEquals(OutboxEventStatus.PENDING, persisted.getStatus());
    assertEquals(1, persisted.getAttempts());
    assertEquals("Kafka unavailable", persisted.getLastError());
    assertEquals(BASE_TIME.plusSeconds(30), persisted.getAvailableAt());
  }

  @Test
  void shouldPersistPublishedState() {
    OutboxEvent event = createEvent(EVENT_ID, BASE_TIME);

    OffsetDateTime publishedAt = BASE_TIME.plusSeconds(5);

    event.markPublished(publishedAt);

    outboxEventRepository.saveAndFlush(event);

    OutboxEvent persisted = outboxEventRepository.findById(EVENT_ID).orElseThrow();

    assertEquals(OutboxEventStatus.PUBLISHED, persisted.getStatus());
    assertEquals(publishedAt, persisted.getPublishedAt());
    assertTrue(persisted.getLastError() == null);
  }

  private OutboxEvent createEvent(UUID eventId, OffsetDateTime occurredAt) {
    return OutboxEvent.create(
        eventId, AGGREGATE_ID, "TRANSFER_COMPLETED", "{\"status\":\"COMPLETED\"}", occurredAt);
  }
}
