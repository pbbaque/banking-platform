package com.pbbaque.banking.audit.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pbbaque.banking.audit.config.SecurityConfig;
import com.pbbaque.banking.audit.domain.AuditOutcome;
import com.pbbaque.banking.audit.dto.AuditEventResponse;
import com.pbbaque.banking.audit.exception.AuditEventNotFoundException;
import com.pbbaque.banking.audit.service.AuditEventService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Web layer tests for {@link AuditEventController}. */
@WebMvcTest(AuditEventController.class)
@Import(SecurityConfig.class)
class AuditEventControllerTest {

  private static final UUID ACTOR_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

  private static final String ACCESS_TOKEN = "test-access-token";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuditEventService auditEventService;

  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void shouldReturnAuditEventById() throws Exception {

    UUID id = UUID.randomUUID();

    AuditEventResponse response = createResponse(id, ACTOR_ID);

    when(auditEventService.getById(id, ACTOR_ID)).thenReturn(response);

    mockMvc
        .perform(get("/api/audit-events/{id}", id).with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.actorId").value(ACTOR_ID.toString()))
        .andExpect(jsonPath("$.eventType").value("TRANSFER_COMPLETED"));
  }

  @Test
  void shouldReturnNotFoundWhenAccessibleAuditEventDoesNotExist() throws Exception {

    UUID id = UUID.randomUUID();

    when(auditEventService.getById(id, ACTOR_ID)).thenThrow(new AuditEventNotFoundException(id));

    mockMvc
        .perform(get("/api/audit-events/{id}", id).with(userJwt()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Audit event not found: " + id));
  }

  @Test
  void shouldReturnAuditEventsByAggregate() throws Exception {

    AuditEventResponse first = createResponse(UUID.randomUUID(), ACTOR_ID);

    AuditEventResponse second = createResponse(UUID.randomUUID(), ACTOR_ID);

    when(auditEventService.getByAggregate("TRANSFER", "transfer-123", ACTOR_ID))
        .thenReturn(List.of(first, second));

    mockMvc
        .perform(
            get("/api/audit-events")
                .with(userJwt())
                .param("aggregateType", "TRANSFER")
                .param("aggregateId", "transfer-123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].aggregateType").value("TRANSFER"))
        .andExpect(jsonPath("$[0].aggregateId").value("transfer-123"));
  }

  @Test
  void shouldReturnAuditEventsByActor() throws Exception {

    AuditEventResponse response = createResponse(UUID.randomUUID(), ACTOR_ID);

    when(auditEventService.getByActor(ACTOR_ID)).thenReturn(List.of(response));

    mockMvc
        .perform(get("/api/audit-events").with(userJwt()).param("actorId", ACTOR_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].actorId").value(ACTOR_ID.toString()));
  }

  @Test
  void shouldRejectAuditEventsForDifferentActor() throws Exception {

    UUID differentActorId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    mockMvc
        .perform(
            get("/api/audit-events").with(userJwt()).param("actorId", differentActorId.toString()))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnAuditEventsByEventType() throws Exception {

    AuditEventResponse response = createResponse(UUID.randomUUID(), ACTOR_ID);

    when(auditEventService.getByEventType("TRANSFER_COMPLETED", ACTOR_ID))
        .thenReturn(List.of(response));

    mockMvc
        .perform(get("/api/audit-events").with(userJwt()).param("eventType", "TRANSFER_COMPLETED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].eventType").value("TRANSFER_COMPLETED"));
  }

  @Test
  void shouldRejectInvalidActorId() throws Exception {

    mockMvc
        .perform(get("/api/audit-events").with(userJwt()).param("actorId", "invalid-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Invalid value for parameter: actorId"));
  }

  @Test
  void shouldRejectAuditEndpointsWithoutJwt() throws Exception {

    mockMvc
        .perform(get("/api/audit-events").param("actorId", ACTOR_ID.toString()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldRejectHttpAuditEventCreation() throws Exception {

    mockMvc.perform(post("/api/audit-events").with(userJwt())).andExpect(status().isForbidden());
  }

  private JwtRequestPostProcessor userJwt() {
    return jwt()
        .jwt(token -> token.tokenValue(ACCESS_TOKEN).subject(ACTOR_ID.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  private AuditEventResponse createResponse(UUID id, UUID actorId) {

    OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);

    return new AuditEventResponse(
        id,
        "TRANSFER_COMPLETED",
        "TRANSFER",
        "transfer-123",
        actorId,
        AuditOutcome.SUCCESS,
        "Transfer completed successfully",
        "{\"amount\":100}",
        occurredAt,
        occurredAt.plusSeconds(1));
  }
}
