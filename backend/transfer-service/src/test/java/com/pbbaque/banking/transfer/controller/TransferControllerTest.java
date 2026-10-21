package com.pbbaque.banking.transfer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbbaque.banking.transfer.config.SecurityConfig;
import com.pbbaque.banking.transfer.domain.Transfer;
import com.pbbaque.banking.transfer.dto.CreateTransferRequest;
import com.pbbaque.banking.transfer.exception.TransferNotFoundException;
import com.pbbaque.banking.transfer.service.TransferService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Web layer tests for {@link TransferController}. */
@WebMvcTest(TransferController.class)
@Import(SecurityConfig.class)
class TransferControllerTest {

  private static final UUID SOURCE_ACCOUNT_ID =
      UUID.fromString("11111111-1111-1111-1111-111111111111");

  private static final UUID DESTINATION_ACCOUNT_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");

  private static final UUID TRANSFER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  private static final UUID IDENTITY_USER_ID =
      UUID.fromString("44444444-4444-4444-4444-444444444444");

  private static final String IDEMPOTENCY_KEY = "transfer-key-1";

  private static final String ACCESS_TOKEN = "test-access-token";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private TransferService transferService;

  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void shouldCreateTransfer() throws Exception {
    CreateTransferRequest request =
        new CreateTransferRequest(
            SOURCE_ACCOUNT_ID,
            DESTINATION_ACCOUNT_ID,
            new BigDecimal("100.0000"),
            "EUR",
            IDEMPOTENCY_KEY);

    Transfer transfer =
        Transfer.create(
            SOURCE_ACCOUNT_ID,
            DESTINATION_ACCOUNT_ID,
            new BigDecimal("100.0000"),
            "EUR",
            IDEMPOTENCY_KEY);

    transfer.complete();

    when(transferService.createTransfer(any(CreateTransferRequest.class), eq(ACCESS_TOKEN)))
        .thenReturn(transfer);

    mockMvc
        .perform(
            post("/api/transfers")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sourceAccountId").value(SOURCE_ACCOUNT_ID.toString()))
        .andExpect(jsonPath("$.destinationAccountId").value(DESTINATION_ACCOUNT_ID.toString()))
        .andExpect(jsonPath("$.amount").value(100.0000))
        .andExpect(jsonPath("$.currency").value("EUR"))
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.idempotencyKey").value(IDEMPOTENCY_KEY));
  }

  @Test
  void shouldReturnTransferById() throws Exception {
    Transfer transfer =
        Transfer.create(
            SOURCE_ACCOUNT_ID,
            DESTINATION_ACCOUNT_ID,
            new BigDecimal("50.0000"),
            "EUR",
            IDEMPOTENCY_KEY);

    when(transferService.getById(TRANSFER_ID, ACCESS_TOKEN)).thenReturn(transfer);

    mockMvc
        .perform(get("/api/transfers/{id}", TRANSFER_ID).with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sourceAccountId").value(SOURCE_ACCOUNT_ID.toString()))
        .andExpect(jsonPath("$.destinationAccountId").value(DESTINATION_ACCOUNT_ID.toString()))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void shouldReturnTransferByIdempotencyKey() throws Exception {
    Transfer transfer =
        Transfer.create(
            SOURCE_ACCOUNT_ID,
            DESTINATION_ACCOUNT_ID,
            new BigDecimal("75.0000"),
            "EUR",
            IDEMPOTENCY_KEY);

    when(transferService.getByIdempotencyKey(IDEMPOTENCY_KEY, ACCESS_TOKEN)).thenReturn(transfer);

    mockMvc
        .perform(get("/api/transfers").with(userJwt()).param("idempotencyKey", IDEMPOTENCY_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.idempotencyKey").value(IDEMPOTENCY_KEY))
        .andExpect(jsonPath("$.amount").value(75.0000))
        .andExpect(jsonPath("$.currency").value("EUR"));
  }

  @Test
  void shouldReturnNotFoundWhenTransferDoesNotExist() throws Exception {
    when(transferService.getById(TRANSFER_ID, ACCESS_TOKEN))
        .thenThrow(new TransferNotFoundException(TRANSFER_ID));

    mockMvc
        .perform(get("/api/transfers/{id}", TRANSFER_ID).with(userJwt()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Transfer not found: " + TRANSFER_ID));
  }

  @Test
  void shouldRejectInvalidTransferRequest() throws Exception {
    CreateTransferRequest request =
        new CreateTransferRequest(
            SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, BigDecimal.ZERO, "", "");

    mockMvc
        .perform(
            post("/api/transfers")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"));
  }

  @Test
  void shouldRejectInvalidTransferId() throws Exception {
    mockMvc
        .perform(get("/api/transfers/not-a-uuid").with(userJwt()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"));
  }

  @Test
  void shouldRejectTransferEndpointWithoutJwt() throws Exception {
    mockMvc.perform(get("/api/transfers/{id}", TRANSFER_ID)).andExpect(status().isUnauthorized());
  }

  private JwtRequestPostProcessor userJwt() {
    return jwt()
        .jwt(token -> token.tokenValue(ACCESS_TOKEN).subject(IDENTITY_USER_ID.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }
}
