package com.pbbaque.banking.audit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbbaque.banking.audit.domain.AuditOutcome;
import com.pbbaque.banking.audit.dto.AuditEventResponse;
import com.pbbaque.banking.audit.dto.CreateAuditEventRequest;
import com.pbbaque.banking.audit.exception.AuditEventNotFoundException;
import com.pbbaque.banking.audit.exception.GlobalExceptionHandler;
import com.pbbaque.banking.audit.service.AuditEventService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/** Unit tests for {@link AuditEventController}. */
@ExtendWith(MockitoExtension.class)
class AuditEventControllerTest {

  @Mock private AuditEventService auditEventService;

  @InjectMocks private AuditEventController auditEventController;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    mockMvc =
        MockMvcBuilders.standaloneSetup(auditEventController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .build();

    objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();
  }

  @Test
  void shouldCreateAuditEvent() throws Exception {
    UUID id = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();
    OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);
    OffsetDateTime createdAt = occurredAt.plusSeconds(1);

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

    AuditEventResponse response =
        new AuditEventResponse(
            id,
            "TRANSFER_COMPLETED",
            "TRANSFER",
            "transfer-123",
            actorId,
            AuditOutcome.SUCCESS,
            "Transfer completed successfully",
            "{\"amount\":100}",
            occurredAt,
            createdAt);

    when(auditEventService.create(any(CreateAuditEventRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/audit-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.eventType").value("TRANSFER_COMPLETED"))
        .andExpect(jsonPath("$.aggregateType").value("TRANSFER"))
        .andExpect(jsonPath("$.aggregateId").value("transfer-123"))
        .andExpect(jsonPath("$.actorId").value(actorId.toString()))
        .andExpect(jsonPath("$.outcome").value("SUCCESS"))
        .andExpect(jsonPath("$.description").value("Transfer completed successfully"));
  }

  @Test
  void shouldReturnAuditEventById() throws Exception {
    UUID id = UUID.randomUUID();

    AuditEventResponse response = createResponse(id);

    when(auditEventService.getById(id)).thenReturn(response);

    mockMvc
        .perform(get("/api/audit-events/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.eventType").value("TRANSFER_COMPLETED"));
  }

  @Test
  void shouldReturnNotFoundWhenAuditEventDoesNotExist() throws Exception {
    UUID id = UUID.randomUUID();

    when(auditEventService.getById(id)).thenThrow(new AuditEventNotFoundException(id));

    mockMvc
        .perform(get("/api/audit-events/{id}", id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Audit event not found: " + id));
  }

  @Test
  void shouldReturnAuditEventsByAggregate() throws Exception {
    AuditEventResponse first = createResponse(UUID.randomUUID());
    AuditEventResponse second = createResponse(UUID.randomUUID());

    when(auditEventService.getByAggregate("TRANSFER", "transfer-123"))
        .thenReturn(List.of(first, second));

    mockMvc
        .perform(
            get("/api/audit-events")
                .param("aggregateType", "TRANSFER")
                .param("aggregateId", "transfer-123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].aggregateType").value("TRANSFER"))
        .andExpect(jsonPath("$[0].aggregateId").value("transfer-123"));
  }

  @Test
  void shouldReturnAuditEventsByActor() throws Exception {
    UUID actorId = UUID.randomUUID();

    AuditEventResponse response =
        new AuditEventResponse(
            UUID.randomUUID(),
            "ACCOUNT_CREATED",
            "ACCOUNT",
            "account-123",
            actorId,
            AuditOutcome.SUCCESS,
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC),
            OffsetDateTime.now(ZoneOffset.UTC));

    when(auditEventService.getByActor(actorId)).thenReturn(List.of(response));

    mockMvc
        .perform(get("/api/audit-events").param("actorId", actorId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].actorId").value(actorId.toString()));
  }

  @Test
  void shouldReturnAuditEventsByEventType() throws Exception {
    AuditEventResponse response = createResponse(UUID.randomUUID());

    when(auditEventService.getByEventType("TRANSFER_COMPLETED")).thenReturn(List.of(response));

    mockMvc
        .perform(get("/api/audit-events").param("eventType", "TRANSFER_COMPLETED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].eventType").value("TRANSFER_COMPLETED"));
  }

  @Test
  void shouldRejectInvalidCreateRequest() throws Exception {
    String invalidRequest =
        """
        {
          "eventType": "",
          "aggregateType": "TRANSFER",
          "aggregateId": "transfer-123",
          "outcome": "SUCCESS",
          "occurredAt": "2026-10-19T00:00:00Z"
        }
        """;

    mockMvc
        .perform(
            post("/api/audit-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"));
  }

  @Test
  void shouldRejectInvalidActorId() throws Exception {
    mockMvc
        .perform(get("/api/audit-events").param("actorId", "invalid-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Invalid value for parameter: actorId"));
  }

  private AuditEventResponse createResponse(UUID id) {
    OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);

    return new AuditEventResponse(
        id,
        "TRANSFER_COMPLETED",
        "TRANSFER",
        "transfer-123",
        UUID.randomUUID(),
        AuditOutcome.SUCCESS,
        "Transfer completed successfully",
        "{\"amount\":100}",
        occurredAt,
        occurredAt.plusSeconds(1));
  }
}
