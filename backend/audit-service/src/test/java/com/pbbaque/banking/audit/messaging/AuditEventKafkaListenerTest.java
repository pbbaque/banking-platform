package com.pbbaque.banking.audit.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbbaque.banking.audit.domain.AuditOutcome;
import com.pbbaque.banking.audit.dto.AuditEventMessage;
import com.pbbaque.banking.audit.service.AuditEventService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link AuditEventKafkaListener}. */
@ExtendWith(MockitoExtension.class)
class AuditEventKafkaListenerTest {

  private static final UUID EVENT_ID = UUID.fromString("7cf72f3a-8de9-4c76-ab49-524cb4a3362a");

  private static final UUID TRANSFER_ID = UUID.fromString("d365d07b-ba86-433f-874d-aaeb9d286f57");

  @Mock private AuditEventService auditEventService;

  private AuditEventKafkaListener listener;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    listener = new AuditEventKafkaListener(auditEventService, objectMapper);
  }

  @Test
  void shouldDeserializeAndDelegateAuditEvent() {
    when(auditEventService.createFromMessage(any(AuditEventMessage.class))).thenReturn(true);

    String payload =
        """
        {
          "eventId": "7cf72f3a-8de9-4c76-ab49-524cb4a3362a",
          "eventType": "TRANSFER_COMPLETED",
          "aggregateType": "TRANSFER",
          "aggregateId": "d365d07b-ba86-433f-874d-aaeb9d286f57",
          "actorId": null,
          "outcome": "SUCCESS",
          "description": "Transfer completed successfully",
          "metadata": {
            "sourceAccountId": "cb5e47a2-47ae-49b5-9b34-5da5433786b5",
            "destinationAccountId": "0f1ffb06-3b9b-4aae-9c53-ffbe60fcc5f1",
            "amount": 25,
            "currency": "EUR",
            "status": "COMPLETED",
            "idempotencyKey": "e2e-outbox-kafka-001"
          },
          "occurredAt": "2026-10-19T09:13:08.5295827Z"
        }
        """;

    listener.consume(payload);

    ArgumentCaptor<AuditEventMessage> captor = ArgumentCaptor.forClass(AuditEventMessage.class);

    verify(auditEventService).createFromMessage(captor.capture());

    AuditEventMessage message = captor.getValue();

    assertEquals(EVENT_ID, message.eventId());
    assertEquals("TRANSFER_COMPLETED", message.eventType());
    assertEquals("TRANSFER", message.aggregateType());
    assertEquals(TRANSFER_ID.toString(), message.aggregateId());
    assertEquals(AuditOutcome.SUCCESS, message.outcome());
    assertEquals("Transfer completed successfully", message.description());
    assertEquals("cb5e47a2-47ae-49b5-9b34-5da5433786b5", message.metadata().get("sourceAccountId"));
    assertEquals(
        "0f1ffb06-3b9b-4aae-9c53-ffbe60fcc5f1", message.metadata().get("destinationAccountId"));
    assertEquals(
        0,
        new BigDecimal("25")
            .compareTo(new BigDecimal(message.metadata().get("amount").toString())));
    assertEquals("EUR", message.metadata().get("currency"));
    assertEquals("COMPLETED", message.metadata().get("status"));
    assertEquals("e2e-outbox-kafka-001", message.metadata().get("idempotencyKey"));
    assertEquals(
        OffsetDateTime.of(2026, 10, 19, 9, 13, 8, 529582700, ZoneOffset.UTC), message.occurredAt());
  }

  @Test
  void shouldThrowWhenPayloadIsInvalidJson() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> listener.consume("{invalid-json}"));

    assertEquals("Unable to deserialize audit event message", exception.getMessage());
  }
}
