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
  void shouldSaveAndFindAuditEventById() {
    AuditEvent auditEvent =
        AuditEvent.create(
            "TRANSFER_COMPLETED",
            "TRANSFER",
            UUID.randomUUID().toString(),
            UUID.randomUUID(),
            AuditOutcome.SUCCESS,
            "Transfer completed successfully",
            "{\"amount\":100}",
            OffsetDateTime.now(ZoneOffset.UTC));

    AuditEvent saved = auditEventRepository.saveAndFlush(auditEvent);

    assertTrue(auditEventRepository.findById(saved.getId()).isPresent());

    AuditEvent found = auditEventRepository.findById(saved.getId()).orElseThrow();

    assertEquals(saved.getId(), found.getId());
    assertEquals("TRANSFER_COMPLETED", found.getEventType());
    assertEquals("TRANSFER", found.getAggregateType());
    assertEquals(AuditOutcome.SUCCESS, found.getOutcome());
  }

  @Test
  void shouldFindAuditEventsByAggregate() {
    String aggregateId = UUID.randomUUID().toString();

    AuditEvent first =
        AuditEvent.create(
            "TRANSFER_CREATED",
            "TRANSFER",
            aggregateId,
            null,
            AuditOutcome.SUCCESS,
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    AuditEvent second =
        AuditEvent.create(
            "TRANSFER_COMPLETED",
            "TRANSFER",
            aggregateId,
            null,
            AuditOutcome.SUCCESS,
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    auditEventRepository.saveAllAndFlush(List.of(first, second));

    List<AuditEvent> results =
        auditEventRepository.findAllByAggregateTypeAndAggregateId("TRANSFER", aggregateId);

    assertEquals(2, results.size());
    assertTrue(results.stream().allMatch(event -> aggregateId.equals(event.getAggregateId())));
  }

  @Test
  void shouldFindAuditEventsByActor() {
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

    auditEventRepository.saveAllAndFlush(List.of(first, second));

    List<AuditEvent> results = auditEventRepository.findAllByActorId(actorId);

    assertEquals(2, results.size());
    assertTrue(results.stream().allMatch(event -> actorId.equals(event.getActorId())));
  }

  @Test
  void shouldFindAuditEventsByEventType() {
    AuditEvent first =
        AuditEvent.create(
            "TRANSFER_FAILED",
            "TRANSFER",
            UUID.randomUUID().toString(),
            null,
            AuditOutcome.FAILURE,
            "Destination credit failed",
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    AuditEvent second =
        AuditEvent.create(
            "TRANSFER_FAILED",
            "TRANSFER",
            UUID.randomUUID().toString(),
            null,
            AuditOutcome.FAILURE,
            "Insufficient funds",
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    auditEventRepository.saveAllAndFlush(List.of(first, second));

    List<AuditEvent> results = auditEventRepository.findAllByEventType("TRANSFER_FAILED");

    assertEquals(2, results.size());
    assertTrue(results.stream().allMatch(event -> "TRANSFER_FAILED".equals(event.getEventType())));
  }

  @Test
  void shouldReturnEmptyListWhenAggregateDoesNotExist() {
    List<AuditEvent> results =
        auditEventRepository.findAllByAggregateTypeAndAggregateId("TRANSFER", "missing-transfer");

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
