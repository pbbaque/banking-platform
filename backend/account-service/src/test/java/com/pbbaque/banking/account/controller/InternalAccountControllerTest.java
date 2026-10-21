package com.pbbaque.banking.account.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbbaque.banking.account.config.SecurityConfig;
import com.pbbaque.banking.account.domain.Account;
import com.pbbaque.banking.account.domain.AccountType;
import com.pbbaque.banking.account.dto.AmountRequest;
import com.pbbaque.banking.account.service.AccountService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Web layer tests for {@link InternalAccountController}. */
@WebMvcTest(InternalAccountController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "security.internal.token=test-internal-service-token")
class InternalAccountControllerTest {

  private static final UUID ACCOUNT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  private static final UUID OPERATION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  private static final String IBAN = "ES9121000418450200051332";

  private static final String INTERNAL_SERVICE_TOKEN = "test-internal-service-token";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AccountService accountService;

  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void shouldGetAccountWithInternalServiceToken() throws Exception {
    when(accountService.getById(ACCOUNT_ID)).thenReturn(createAccount());

    mockMvc
        .perform(
            get("/api/internal/accounts/{id}", ACCOUNT_ID)
                .header("X-Internal-Service-Token", INTERNAL_SERVICE_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()))
        .andExpect(jsonPath("$.iban").value(IBAN));
  }

  @Test
  void shouldCreditAccountWithInternalServiceToken() throws Exception {
    Account account = createAccount();
    account.credit(new BigDecimal("100.00"));

    when(accountService.credit(ACCOUNT_ID, new BigDecimal("100.00"), OPERATION_ID))
        .thenReturn(account);

    AmountRequest request = new AmountRequest(new BigDecimal("100.00"));

    mockMvc
        .perform(
            post("/api/internal/accounts/{id}/credit", ACCOUNT_ID)
                .header("X-Internal-Service-Token", INTERNAL_SERVICE_TOKEN)
                .header("Idempotency-Key", OPERATION_ID.toString())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(100.00));
  }

  @Test
  void shouldDebitAccountWithInternalServiceToken() throws Exception {
    Account account = createAccount();
    account.credit(new BigDecimal("100.00"));
    account.debit(new BigDecimal("25.00"));

    when(accountService.debit(ACCOUNT_ID, new BigDecimal("25.00"), OPERATION_ID))
        .thenReturn(account);

    AmountRequest request = new AmountRequest(new BigDecimal("25.00"));

    mockMvc
        .perform(
            post("/api/internal/accounts/{id}/debit", ACCOUNT_ID)
                .header("X-Internal-Service-Token", INTERNAL_SERVICE_TOKEN)
                .header("Idempotency-Key", OPERATION_ID.toString())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(75.00));
  }

  @Test
  void shouldRejectInternalEndpointWithoutServiceToken() throws Exception {
    mockMvc
        .perform(get("/api/internal/accounts/{id}", ACCOUNT_ID))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldRejectInternalEndpointWithInvalidServiceToken() throws Exception {
    mockMvc
        .perform(
            get("/api/internal/accounts/{id}", ACCOUNT_ID)
                .header("X-Internal-Service-Token", "invalid-token"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldRejectMissingIdempotencyKey() throws Exception {
    AmountRequest request = new AmountRequest(new BigDecimal("25.00"));

    mockMvc
        .perform(
            post("/api/internal/accounts/{id}/debit", ACCOUNT_ID)
                .header("X-Internal-Service-Token", INTERNAL_SERVICE_TOKEN)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRejectInvalidAmount() throws Exception {
    AmountRequest request = new AmountRequest(BigDecimal.ZERO);

    mockMvc
        .perform(
            post("/api/internal/accounts/{id}/credit", ACCOUNT_ID)
                .header("X-Internal-Service-Token", INTERNAL_SERVICE_TOKEN)
                .header("Idempotency-Key", OPERATION_ID.toString())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  private Account createAccount() {
    return Account.create(CUSTOMER_ID, IBAN, AccountType.CHECKING, "EUR");
  }
}
