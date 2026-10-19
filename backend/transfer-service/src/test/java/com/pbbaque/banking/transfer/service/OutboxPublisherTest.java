package com.pbbaque.banking.transfer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pbbaque.banking.transfer.config.AuditEventsTopicProperties;
import com.pbbaque.banking.transfer.config.OutboxPublisherProperties;
import com.pbbaque.banking.transfer.domain.OutboxEvent;
import com.pbbaque.banking.transfer.domain.OutboxEventStatus;
import com.pbbaque.banking.transfer.repository.OutboxEventRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

/** Unit tests for {@link OutboxPublisher}. */
@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

  private static final String TOPIC_NAME = "banking.audit-events";

  private static final UUID EVENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

  private static final UUID AGGREGATE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

  private static final String PAYLOAD = "{\"eventId\":\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\"}";

  @Mock private OutboxEventRepository outboxEventRepository;

  @Mock private KafkaTemplate<String, String> kafkaTemplate;

  private OutboxPublisher outboxPublisher;

  @BeforeEach
  void setUp() {
    AuditEventsTopicProperties topicProperties =
        new AuditEventsTopicProperties(TOPIC_NAME, 3, (short) 1);

    OutboxPublisherProperties publisherProperties =
        new OutboxPublisherProperties(
            100,
            java.time.Duration.ofSeconds(10),
            java.time.Duration.ofSeconds(30),
            java.time.Duration.ofSeconds(5));

    outboxPublisher =
        new OutboxPublisher(
            outboxEventRepository, kafkaTemplate, topicProperties, publisherProperties);
  }

  @Test
  void shouldDoNothingWhenNoPendingEventsExist() {
    when(outboxEventRepository.findByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            eq(OutboxEventStatus.PENDING), any(OffsetDateTime.class), any(Pageable.class)))
        .thenReturn(List.of());

    outboxPublisher.publishPendingEvents();

    verify(kafkaTemplate, never()).send(any(), any(), any());
    verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
  }

  @Test
  void shouldPublishPendingEventAndMarkItAsPublished() throws Exception {
    OutboxEvent event = createEvent();

    when(outboxEventRepository.findByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            eq(OutboxEventStatus.PENDING), any(OffsetDateTime.class), any(Pageable.class)))
        .thenReturn(List.of(event));

    when(kafkaTemplate.send(TOPIC_NAME, AGGREGATE_ID.toString(), PAYLOAD))
        .thenReturn(CompletableFuture.completedFuture(null));

    outboxPublisher.publishPendingEvents();

    verify(kafkaTemplate).send(TOPIC_NAME, AGGREGATE_ID.toString(), PAYLOAD);

    ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository).save(captor.capture());

    OutboxEvent persisted = captor.getValue();

    assertEquals(OutboxEventStatus.PUBLISHED, persisted.getStatus());
    assertNotNull(persisted.getPublishedAt());
    assertEquals(0, persisted.getAttempts());
    assertNull(persisted.getLastError());
  }

  @Test
  void shouldKeepEventPendingAndScheduleRetryWhenKafkaFails() {
    OutboxEvent event = createEvent();

    when(outboxEventRepository.findByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            eq(OutboxEventStatus.PENDING), any(OffsetDateTime.class), any(Pageable.class)))
        .thenReturn(List.of(event));

    CompletableFuture<org.springframework.kafka.support.SendResult<String, String>> failedFuture =
        new CompletableFuture<>();

    failedFuture.completeExceptionally(new RuntimeException("Kafka unavailable"));

    when(kafkaTemplate.send(TOPIC_NAME, AGGREGATE_ID.toString(), PAYLOAD)).thenReturn(failedFuture);

    OffsetDateTime beforePublishing = OffsetDateTime.now(ZoneOffset.UTC);

    outboxPublisher.publishPendingEvents();

    verify(kafkaTemplate).send(TOPIC_NAME, AGGREGATE_ID.toString(), PAYLOAD);

    ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository).save(captor.capture());

    OutboxEvent persisted = captor.getValue();

    assertEquals(OutboxEventStatus.PENDING, persisted.getStatus());
    assertEquals(1, persisted.getAttempts());
    assertNotNull(persisted.getLastError());
    assertTrue(persisted.getLastError().contains("Kafka unavailable"));
    assertTrue(persisted.getAvailableAt().isAfter(beforePublishing));
    assertNull(persisted.getPublishedAt());
  }

  @Test
  void shouldContinuePublishingRemainingEventsWhenOneEventFails() {
    OutboxEvent failedEvent =
        OutboxEvent.create(
            EVENT_ID,
            AGGREGATE_ID,
            "TRANSFER_COMPLETED",
            PAYLOAD,
            OffsetDateTime.now(ZoneOffset.UTC));

    UUID secondEventId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    UUID secondAggregateId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    String secondPayload = "{\"eventId\":\"cccccccc-cccc-cccc-cccc-cccccccccccc\"}";

    OutboxEvent successfulEvent =
        OutboxEvent.create(
            secondEventId,
            secondAggregateId,
            "TRANSFER_FAILED",
            secondPayload,
            OffsetDateTime.now(ZoneOffset.UTC));

    when(outboxEventRepository.findByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            eq(OutboxEventStatus.PENDING), any(OffsetDateTime.class), any(Pageable.class)))
        .thenReturn(List.of(failedEvent, successfulEvent));

    CompletableFuture<org.springframework.kafka.support.SendResult<String, String>> failedFuture =
        new CompletableFuture<>();

    failedFuture.completeExceptionally(new RuntimeException("Kafka unavailable"));

    when(kafkaTemplate.send(TOPIC_NAME, AGGREGATE_ID.toString(), PAYLOAD)).thenReturn(failedFuture);

    when(kafkaTemplate.send(TOPIC_NAME, secondAggregateId.toString(), secondPayload))
        .thenReturn(CompletableFuture.completedFuture(null));

    outboxPublisher.publishPendingEvents();

    verify(kafkaTemplate).send(TOPIC_NAME, AGGREGATE_ID.toString(), PAYLOAD);
    verify(kafkaTemplate).send(TOPIC_NAME, secondAggregateId.toString(), secondPayload);

    assertEquals(OutboxEventStatus.PENDING, failedEvent.getStatus());
    assertEquals(1, failedEvent.getAttempts());

    assertEquals(OutboxEventStatus.PUBLISHED, successfulEvent.getStatus());
    assertNotNull(successfulEvent.getPublishedAt());

    verify(outboxEventRepository).save(failedEvent);
    verify(outboxEventRepository).save(successfulEvent);
  }

  private OutboxEvent createEvent() {
    return OutboxEvent.create(
        EVENT_ID, AGGREGATE_ID, "TRANSFER_COMPLETED", PAYLOAD, OffsetDateTime.now(ZoneOffset.UTC));
  }
}
