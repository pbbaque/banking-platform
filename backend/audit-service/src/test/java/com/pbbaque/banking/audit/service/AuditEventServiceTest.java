package com.pbbaque.banking.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbbaque.banking.audit.domain.AuditEvent;
import com.pbbaque.banking.audit.domain.AuditOutcome;
import com.pbbaque.banking.audit.dto.AuditEventMessage;
import com.pbbaque.banking.audit.dto.AuditEventResponse;
import com.pbbaque.banking.audit.dto.CreateAuditEventRequest;
import com.pbbaque.banking.audit.exception.AuditEventNotFoundException;
import com.pbbaque.banking.audit.repository.AuditEventRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link AuditEventService}. */
@ExtendWith(MockitoExtension.class)
class AuditEventServiceTest {

  private static final UUID EVENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

  private static final UUID ACTOR_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

  private static final OffsetDateTime OCCURRED_AT =
      OffsetDateTime.of(2026, 10, 19, 9, 13, 8, 0, ZoneOffset.UTC);

  @Mock private AuditEventRepository auditEventRepository;

  private ObjectMapper objectMapper;

  private AuditEventService auditEventService;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();
    auditEventService = new AuditEventService(auditEventRepository, objectMapper);
  }

  @Test
  void shouldCreateAuditEvent() {
    UUID actorId = UUID.randomUUID();
    OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);

    CreateAuditEventRequest request =
        new CreateAuditEventRequest(
            "TRANSFER_COMPLETED",
            "TRANSFER",
            "transfer-123",
            actorId,
            AuditOutcome.SUCCESS,
            "Transfer completed successfully",
            "{\"amount\":100}",
            occurredAt);

    when(auditEventRepository.save(any(AuditEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AuditEventResponse response = auditEventService.create(request);

    ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);

    verify(auditEventRepository).save(captor.capture());

    AuditEvent savedAuditEvent = captor.getValue();

    assertEquals("TRANSFER_COMPLETED", savedAuditEvent.getEventType());
    assertEquals("TRANSFER", savedAuditEvent.getAggregateType());
    assertEquals("transfer-123", savedAuditEvent.getAggregateId());
    assertEquals(actorId, savedAuditEvent.getActorId());
    assertEquals(AuditOutcome.SUCCESS, savedAuditEvent.getOutcome());
    assertEquals("Transfer completed successfully", savedAuditEvent.getDescription());
    assertEquals("{\"amount\":100}", savedAuditEvent.getMetadata());
    assertEquals(occurredAt, savedAuditEvent.getOccurredAt());

    assertEquals(savedAuditEvent.getId(), response.id());
    assertEquals(savedAuditEvent.getEventType(), response.eventType());
    assertEquals(savedAuditEvent.getAggregateType(), response.aggregateType());
    assertEquals(savedAuditEvent.getAggregateId(), response.aggregateId());
    assertEquals(savedAuditEvent.getActorId(), response.actorId());
    assertEquals(savedAuditEvent.getOutcome(), response.outcome());
    assertEquals(savedAuditEvent.getDescription(), response.description());
    assertEquals(savedAuditEvent.getMetadata(), response.metadata());
    assertEquals(savedAuditEvent.getOccurredAt(), response.occurredAt());
  }

  @Test
  void shouldCreateAuditEventFromMessage() throws Exception {
    AuditEventMessage message = createMessage();

    when(auditEventRepository.existsById(EVENT_ID)).thenReturn(false);

    when(auditEventRepository.save(any(AuditEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    boolean created = auditEventService.createFromMessage(message);

    assertTrue(created);

    verify(auditEventRepository).existsById(EVENT_ID);

    ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditEventRepository).save(captor.capture());

    AuditEvent savedAuditEvent = captor.getValue();

    assertEquals(EVENT_ID, savedAuditEvent.getId());
    assertEquals("TRANSFER_COMPLETED", savedAuditEvent.getEventType());
    assertEquals("TRANSFER", savedAuditEvent.getAggregateType());
    assertEquals("transfer-123", savedAuditEvent.getAggregateId());
    assertEquals(ACTOR_ID, savedAuditEvent.getActorId());
    assertEquals(AuditOutcome.SUCCESS, savedAuditEvent.getOutcome());
    assertEquals("Transfer completed successfully", savedAuditEvent.getDescription());
    assertEquals(OCCURRED_AT, savedAuditEvent.getOccurredAt());

    JsonNode metadata = objectMapper.readTree(savedAuditEvent.getMetadata());

    assertEquals("11111111-1111-1111-1111-111111111111", metadata.get("sourceAccountId").asText());

    assertEquals(
        "22222222-2222-2222-2222-222222222222", metadata.get("destinationAccountId").asText());

    assertEquals(0, new BigDecimal("25").compareTo(metadata.get("amount").decimalValue()));
    assertEquals("EUR", metadata.get("currency").asText());
    assertEquals("COMPLETED", metadata.get("status").asText());
    assertEquals("e2e-outbox-kafka-001", metadata.get("idempotencyKey").asText());
  }

  @Test
  void shouldIgnoreAlreadyProcessedMessage() {
    AuditEventMessage message = createMessage();

    when(auditEventRepository.existsById(EVENT_ID)).thenReturn(true);

    boolean created = auditEventService.createFromMessage(message);

    assertFalse(created);

    verify(auditEventRepository).existsById(EVENT_ID);
    verify(auditEventRepository, never()).save(any(AuditEvent.class));
  }

  @Test
  void shouldPersistNullMetadataWhenMessageMetadataIsEmpty() {
    AuditEventMessage message =
        new AuditEventMessage(
            EVENT_ID,
            "TRANSFER_FAILED",
            "TRANSFER",
            "transfer-123",
            null,
            AuditOutcome.FAILURE,
            "Transfer failed",
            Map.of(),
            OCCURRED_AT);

    when(auditEventRepository.existsById(EVENT_ID)).thenReturn(false);

    when(auditEventRepository.save(any(AuditEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    boolean created = auditEventService.createFromMessage(message);

    assertTrue(created);

    ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditEventRepository).save(captor.capture());

    assertEquals(EVENT_ID, captor.getValue().getId());
    assertEquals(AuditOutcome.FAILURE, captor.getValue().getOutcome());
    assertEquals(null, captor.getValue().getMetadata());
  }

  @Test
  void shouldReturnAuditEventById() {
    AuditEvent auditEvent = createAuditEvent();

    when(auditEventRepository.findById(auditEvent.getId())).thenReturn(Optional.of(auditEvent));

    AuditEventResponse response = auditEventService.getById(auditEvent.getId());

    assertEquals(auditEvent.getId(), response.id());
    assertEquals(auditEvent.getEventType(), response.eventType());
    assertEquals(auditEvent.getAggregateType(), response.aggregateType());
    assertEquals(auditEvent.getAggregateId(), response.aggregateId());

    verify(auditEventRepository).findById(auditEvent.getId());
  }

  @Test
  void shouldThrowWhenAuditEventDoesNotExist() {
    UUID id = UUID.randomUUID();

    when(auditEventRepository.findById(id)).thenReturn(Optional.empty());

    AuditEventNotFoundException exception =
        assertThrows(AuditEventNotFoundException.class, () -> auditEventService.getById(id));

    assertEquals("Audit event not found: " + id, exception.getMessage());

    verify(auditEventRepository).findById(id);
  }

  @Test
  void shouldReturnAuditEventsByAggregate() {
    AuditEvent first =
        AuditEvent.create(
            "TRANSFER_CREATED",
            "TRANSFER",
            "transfer-123",
            null,
            AuditOutcome.SUCCESS,
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    AuditEvent second =
        AuditEvent.create(
            "TRANSFER_COMPLETED",
            "TRANSFER",
            "transfer-123",
            null,
            AuditOutcome.SUCCESS,
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    when(auditEventRepository.findAllByAggregateTypeAndAggregateId("TRANSFER", "transfer-123"))
        .thenReturn(List.of(first, second));

    List<AuditEventResponse> responses =
        auditEventService.getByAggregate("TRANSFER", "transfer-123");

    assertEquals(2, responses.size());
    assertEquals(first.getId(), responses.get(0).id());
    assertEquals(second.getId(), responses.get(1).id());

    verify(auditEventRepository).findAllByAggregateTypeAndAggregateId("TRANSFER", "transfer-123");
  }

  @Test
  void shouldReturnAuditEventsByActor() {
    UUID actorId = UUID.randomUUID();

    AuditEvent first =
        AuditEvent.create(
            "ACCOUNT_CREATED",
            "ACCOUNT",
            UUID.randomUUID().toString(),
            actorId,
            AuditOutcome.SUCCESS,
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    AuditEvent second =
        AuditEvent.create(
            "TRANSFER_CREATED",
            "TRANSFER",
            UUID.randomUUID().toString(),
            actorId,
            AuditOutcome.SUCCESS,
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    when(auditEventRepository.findAllByActorId(actorId)).thenReturn(List.of(first, second));

    List<AuditEventResponse> responses = auditEventService.getByActor(actorId);

    assertEquals(2, responses.size());
    assertEquals(first.getId(), responses.get(0).id());
    assertEquals(second.getId(), responses.get(1).id());

    verify(auditEventRepository).findAllByActorId(actorId);
  }

  @Test
  void shouldReturnAuditEventsByEventType() {
    AuditEvent first =
        AuditEvent.create(
            "TRANSFER_FAILED",
            "TRANSFER",
            UUID.randomUUID().toString(),
            null,
            AuditOutcome.FAILURE,
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    AuditEvent second =
        AuditEvent.create(
            "TRANSFER_FAILED",
            "TRANSFER",
            UUID.randomUUID().toString(),
            null,
            AuditOutcome.FAILURE,
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    when(auditEventRepository.findAllByEventType("TRANSFER_FAILED"))
        .thenReturn(List.of(first, second));

    List<AuditEventResponse> responses = auditEventService.getByEventType("TRANSFER_FAILED");

    assertEquals(2, responses.size());
    assertEquals(first.getId(), responses.get(0).id());
    assertEquals(second.getId(), responses.get(1).id());

    verify(auditEventRepository).findAllByEventType("TRANSFER_FAILED");
  }

  @Test
  void shouldReturnEmptyListWhenNoAggregateEventsExist() {
    when(auditEventRepository.findAllByAggregateTypeAndAggregateId("TRANSFER", "missing-transfer"))
        .thenReturn(List.of());

    List<AuditEventResponse> responses =
        auditEventService.getByAggregate("TRANSFER", "missing-transfer");

    assertEquals(0, responses.size());

    verify(auditEventRepository)
        .findAllByAggregateTypeAndAggregateId("TRANSFER", "missing-transfer");
  }

  private AuditEventMessage createMessage() {
    return new AuditEventMessage(
        EVENT_ID,
        "TRANSFER_COMPLETED",
        "TRANSFER",
        "transfer-123",
        ACTOR_ID,
        AuditOutcome.SUCCESS,
        "Transfer completed successfully",
        Map.of(
            "sourceAccountId", "11111111-1111-1111-1111-111111111111",
            "destinationAccountId", "22222222-2222-2222-2222-222222222222",
            "amount", new BigDecimal("25"),
            "currency", "EUR",
            "status", "COMPLETED",
            "idempotencyKey", "e2e-outbox-kafka-001"),
        OCCURRED_AT);
  }

  private AuditEvent createAuditEvent() {
    return AuditEvent.create(
        "TRANSFER_COMPLETED",
        "TRANSFER",
        "transfer-123",
        UUID.randomUUID(),
        AuditOutcome.SUCCESS,
        "Transfer completed successfully",
        "{\"amount\":100}",
        OffsetDateTime.now(ZoneOffset.UTC));
  }
}
