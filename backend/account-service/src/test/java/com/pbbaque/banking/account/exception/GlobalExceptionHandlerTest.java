package com.pbbaque.banking.account.exception;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbbaque.banking.account.controller.AccountController;
import com.pbbaque.banking.account.controller.InternalAccountController;
import com.pbbaque.banking.account.domain.AccountType;
import com.pbbaque.banking.account.dto.AmountRequest;
import com.pbbaque.banking.account.dto.CreateAccountRequest;
import com.pbbaque.banking.account.service.AccountAuthorizationService;
import com.pbbaque.banking.account.service.AccountService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Web layer tests for {@link GlobalExceptionHandler}. */
@WebMvcTest({AccountController.class, InternalAccountController.class})
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

  private static final UUID ACCOUNT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  private static final UUID OPERATION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  private static final String IBAN = "ES9121000418450200051332";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AccountService accountService;

  @MockitoBean private AccountAuthorizationService accountAuthorizationService;

  @Test
  void shouldReturnNotFoundWhenAccountDoesNotExist() throws Exception {
    when(accountService.getById(ACCOUNT_ID))
        .thenThrow(new AccountNotFoundException("Account not found"));

    mockMvc
        .perform(get("/api/accounts/{id}", ACCOUNT_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Account not found"))
        .andExpect(jsonPath("$.path").value("/api/accounts/" + ACCOUNT_ID));
  }

  @Test
  void shouldReturnConflictWhenAccountAlreadyExists() throws Exception {
    when(accountService.createAccount(CUSTOMER_ID, IBAN, AccountType.CHECKING, "EUR"))
        .thenThrow(new AccountAlreadyExistsException("Account with this IBAN already exists"));

    CreateAccountRequest request =
        new CreateAccountRequest(CUSTOMER_ID, IBAN, AccountType.CHECKING, "EUR");

    mockMvc
        .perform(
            post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.error").value("Conflict"))
        .andExpect(jsonPath("$.message").value("Account with this IBAN already exists"))
        .andExpect(jsonPath("$.path").value("/api/accounts"));
  }

  @Test
  void shouldReturnConflictWhenAccountIsNotActive() throws Exception {
    when(accountService.debit(ACCOUNT_ID, new BigDecimal("10.00"), OPERATION_ID))
        .thenThrow(new AccountNotActiveException("Only active accounts can send funds"));

    AmountRequest request = new AmountRequest(new BigDecimal("10.00"));

    mockMvc
        .perform(
            post("/api/internal/accounts/{id}/debit", ACCOUNT_ID)
                .header("Idempotency-Key", OPERATION_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.message").value("Only active accounts can send funds"));
  }

  @Test
  void shouldReturnConflictWhenFundsAreInsufficient() throws Exception {
    when(accountService.debit(ACCOUNT_ID, new BigDecimal("100.00"), OPERATION_ID))
        .thenThrow(new InsufficientFundsException("Insufficient funds"));

    AmountRequest request = new AmountRequest(new BigDecimal("100.00"));

    mockMvc
        .perform(
            post("/api/internal/accounts/{id}/debit", ACCOUNT_ID)
                .header("Idempotency-Key", OPERATION_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.message").value("Insufficient funds"));
  }

  @Test
  void shouldReturnConflictWhenIdempotencyKeyIsReusedForDifferentOperation() throws Exception {
    when(accountService.debit(ACCOUNT_ID, new BigDecimal("25.00"), OPERATION_ID))
        .thenThrow(
            new IdempotencyConflictException(
                "Idempotency key was already used for a different account operation"));

    AmountRequest request = new AmountRequest(new BigDecimal("25.00"));

    mockMvc
        .perform(
            post("/api/internal/accounts/{id}/debit", ACCOUNT_ID)
                .header("Idempotency-Key", OPERATION_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.error").value("Conflict"))
        .andExpect(
            jsonPath("$.message")
                .value("Idempotency key was already used for a different account operation"))
        .andExpect(jsonPath("$.path").value("/api/internal/accounts/" + ACCOUNT_ID + "/debit"));
  }

  @Test
  void shouldReturnBadRequestForInvalidAmount() throws Exception {
    AmountRequest request = new AmountRequest(BigDecimal.ZERO);

    mockMvc
        .perform(
            post("/api/internal/accounts/{id}/credit", ACCOUNT_ID)
                .header("Idempotency-Key", OPERATION_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message", containsString("amount")));
  }
}
