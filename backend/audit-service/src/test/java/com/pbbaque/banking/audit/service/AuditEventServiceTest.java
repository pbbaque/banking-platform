package com.pbbaque.banking.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pbbaque.banking.audit.domain.AuditEvent;
import com.pbbaque.banking.audit.domain.AuditOutcome;
import com.pbbaque.banking.audit.dto.AuditEventResponse;
import com.pbbaque.banking.audit.dto.CreateAuditEventRequest;
import com.pbbaque.banking.audit.exception.AuditEventNotFoundException;
import com.pbbaque.banking.audit.repository.AuditEventRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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

  @Mock private AuditEventRepository auditEventRepository;

  private AuditEventService auditEventService;

  @BeforeEach
  void setUp() {
    auditEventService = new AuditEventService(auditEventRepository);
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
