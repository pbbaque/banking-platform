package com.pbbaque.banking.audit.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pbbaque.banking.audit.domain.AuditEvent;
import com.pbbaque.banking.audit.domain.AuditOutcome;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/** Integration tests for {@link AuditEventRepository}. */
@DataJpaTest
@ActiveProfiles("local")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuditEventRepositoryIT {

  @Autowired private AuditEventRepository auditEventRepository;

  @AfterEach
  void cleanUp() {
    auditEventRepository.deleteAll();
  }

  @Test
  void shouldSaveAndFindAuditEventByIdAndActor() {
    UUID actorId = UUID.randomUUID();

    AuditEvent auditEvent =
        AuditEvent.create(
            "TRANSFER_COMPLETED",
            "TRANSFER",
            UUID.randomUUID().toString(),
            actorId,
            AuditOutcome.SUCCESS,
            "Transfer completed successfully",
            "{\"amount\":100}",
            OffsetDateTime.now(ZoneOffset.UTC));

    AuditEvent saved = auditEventRepository.saveAndFlush(auditEvent);

    assertTrue(auditEventRepository.findByIdAndActorId(saved.getId(), actorId).isPresent());

    AuditEvent found =
        auditEventRepository.findByIdAndActorId(saved.getId(), actorId).orElseThrow();

    assertEquals(saved.getId(), found.getId());

    assertEquals(actorId, found.getActorId());

    assertEquals("TRANSFER_COMPLETED", found.getEventType());

    assertEquals("TRANSFER", found.getAggregateType());

    assertEquals(AuditOutcome.SUCCESS, found.getOutcome());
  }

  @Test
  void shouldNotFindAuditEventByIdForDifferentActor() {
    UUID ownerActorId = UUID.randomUUID();

    UUID differentActorId = UUID.randomUUID();

    AuditEvent auditEvent =
        AuditEvent.create(
            "TRANSFER_COMPLETED",
            "TRANSFER",
            UUID.randomUUID().toString(),
            ownerActorId,
            AuditOutcome.SUCCESS,
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    AuditEvent saved = auditEventRepository.saveAndFlush(auditEvent);

    assertTrue(auditEventRepository.findByIdAndActorId(saved.getId(), differentActorId).isEmpty());
  }

  @Test
  void shouldFindAuditEventsByAggregateAndActor() {
    UUID actorId = UUID.randomUUID();

    UUID differentActorId = UUID.randomUUID();

    String aggregateId = UUID.randomUUID().toString();

    AuditEvent first =
        AuditEvent.create(
            "TRANSFER_CREATED",
            "TRANSFER",
            aggregateId,
            actorId,
            AuditOutcome.SUCCESS,
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    AuditEvent second =
        AuditEvent.create(
            "TRANSFER_COMPLETED",
            "TRANSFER",
            aggregateId,
            actorId,
            AuditOutcome.SUCCESS,
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    AuditEvent otherActorEvent =
        AuditEvent.create(
            "TRANSFER_COMPLETED",
            "TRANSFER",
            aggregateId,
            differentActorId,
            AuditOutcome.SUCCESS,
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    auditEventRepository.saveAllAndFlush(List.of(first, second, otherActorEvent));

    List<AuditEvent> results =
        auditEventRepository.findAllByAggregateTypeAndAggregateIdAndActorId(
            "TRANSFER", aggregateId, actorId);

    assertEquals(2, results.size());

    assertTrue(
        results.stream()
            .allMatch(
                event ->
                    aggregateId.equals(event.getAggregateId())
                        && actorId.equals(event.getActorId())));
  }

  @Test
  void shouldFindAuditEventsByActor() {
    UUID actorId = UUID.randomUUID();

    UUID differentActorId = UUID.randomUUID();

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

    AuditEvent otherActorEvent =
        AuditEvent.create(
            "TRANSFER_CREATED",
            "TRANSFER",
            UUID.randomUUID().toString(),
            differentActorId,
            AuditOutcome.SUCCESS,
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    auditEventRepository.saveAllAndFlush(List.of(first, second, otherActorEvent));

    List<AuditEvent> results = auditEventRepository.findAllByActorId(actorId);

    assertEquals(2, results.size());

    assertTrue(results.stream().allMatch(event -> actorId.equals(event.getActorId())));
  }

  @Test
  void shouldFindAuditEventsByEventTypeAndActor() {
    UUID actorId = UUID.randomUUID();

    UUID differentActorId = UUID.randomUUID();

    AuditEvent first =
        AuditEvent.create(
            "TRANSFER_FAILED",
            "TRANSFER",
            UUID.randomUUID().toString(),
            actorId,
            AuditOutcome.FAILURE,
            "Destination credit failed",
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    AuditEvent second =
        AuditEvent.create(
            "TRANSFER_FAILED",
            "TRANSFER",
            UUID.randomUUID().toString(),
            actorId,
            AuditOutcome.FAILURE,
            "Insufficient funds",
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    AuditEvent otherActorEvent =
        AuditEvent.create(
            "TRANSFER_FAILED",
            "TRANSFER",
            UUID.randomUUID().toString(),
            differentActorId,
            AuditOutcome.FAILURE,
            "Other actor transfer failed",
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    auditEventRepository.saveAllAndFlush(List.of(first, second, otherActorEvent));

    List<AuditEvent> results =
        auditEventRepository.findAllByEventTypeAndActorId("TRANSFER_FAILED", actorId);

    assertEquals(2, results.size());

    assertTrue(
        results.stream()
            .allMatch(
                event ->
                    "TRANSFER_FAILED".equals(event.getEventType())
                        && actorId.equals(event.getActorId())));
  }

  @Test
  void shouldReturnEmptyListWhenAggregateDoesNotExistForActor() {
    UUID actorId = UUID.randomUUID();

    List<AuditEvent> results =
        auditEventRepository.findAllByAggregateTypeAndAggregateIdAndActorId(
            "TRANSFER", "missing-transfer", actorId);

    assertTrue(results.isEmpty());
  }

  @Test
  void shouldPersistOptionalNullValues() {
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

    AuditEvent saved = auditEventRepository.saveAndFlush(auditEvent);

    AuditEvent found = auditEventRepository.findById(saved.getId()).orElseThrow();

    assertEquals(saved.getId(), found.getId());

    assertEquals(AuditOutcome.SUCCESS, found.getOutcome());

    assertFalse(found.getOccurredAt() == null);

    assertTrue(found.getActorId() == null);

    assertTrue(found.getDescription() == null);

    assertTrue(found.getMetadata() == null);
  }
}
