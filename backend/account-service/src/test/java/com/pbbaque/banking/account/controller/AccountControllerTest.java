package com.pbbaque.banking.account.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbbaque.banking.account.domain.Account;
import com.pbbaque.banking.account.domain.AccountType;
import com.pbbaque.banking.account.dto.AmountRequest;
import com.pbbaque.banking.account.dto.CreateAccountRequest;
import com.pbbaque.banking.account.service.AccountService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Web layer tests for {@link AccountController}. */
@WebMvcTest(AccountController.class)
class AccountControllerTest {

  private static final UUID ACCOUNT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  private static final UUID OPERATION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  private static final String IBAN = "ES9121000418450200051332";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AccountService accountService;

  @Test
  void shouldCreateAccount() throws Exception {
    Account account = createAccount();

    when(accountService.createAccount(CUSTOMER_ID, IBAN, AccountType.CHECKING, "EUR"))
        .thenReturn(account);

    CreateAccountRequest request =
        new CreateAccountRequest(CUSTOMER_ID, IBAN, AccountType.CHECKING, "EUR");

    mockMvc
        .perform(
            post("/api/accounts")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()))
        .andExpect(jsonPath("$.iban").value(IBAN))
        .andExpect(jsonPath("$.accountType").value("CHECKING"))
        .andExpect(jsonPath("$.currency").value("EUR"))
        .andExpect(jsonPath("$.balance").value(0))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void shouldGetAccountById() throws Exception {
    when(accountService.getById(ACCOUNT_ID)).thenReturn(createAccount());

    mockMvc
        .perform(get("/api/accounts/{id}", ACCOUNT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.iban").value(IBAN))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void shouldGetAccountByIban() throws Exception {
    when(accountService.getByIban(IBAN)).thenReturn(createAccount());

    mockMvc
        .perform(get("/api/accounts/iban/{iban}", IBAN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.iban").value(IBAN));
  }

  @Test
  void shouldGetAccountsByCustomerId() throws Exception {
    when(accountService.getByCustomerId(CUSTOMER_ID)).thenReturn(List.of(createAccount()));

    mockMvc
        .perform(get("/api/accounts").param("customerId", CUSTOMER_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].customerId").value(CUSTOMER_ID.toString()))
        .andExpect(jsonPath("$[0].iban").value(IBAN));
  }

  @Test
  void shouldBlockAccount() throws Exception {
    Account account = createAccount();
    account.block();

    when(accountService.block(ACCOUNT_ID)).thenReturn(account);

    mockMvc
        .perform(put("/api/accounts/{id}/block", ACCOUNT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("BLOCKED"));
  }

  @Test
  void shouldActivateAccount() throws Exception {
    Account account = createAccount();

    when(accountService.activate(ACCOUNT_ID)).thenReturn(account);

    mockMvc
        .perform(put("/api/accounts/{id}/activate", ACCOUNT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void shouldCloseAccount() throws Exception {
    Account account = createAccount();
    account.close();

    when(accountService.close(ACCOUNT_ID)).thenReturn(account);

    mockMvc
        .perform(put("/api/accounts/{id}/close", ACCOUNT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"));
  }

  @Test
  void shouldCreditAccount() throws Exception {
    Account account = createAccount();
    account.credit(new BigDecimal("100.00"));

    when(accountService.credit(ACCOUNT_ID, new BigDecimal("100.00"), OPERATION_ID))
        .thenReturn(account);

    AmountRequest request = new AmountRequest(new BigDecimal("100.00"));

    mockMvc
        .perform(
            post("/api/accounts/{id}/credit", ACCOUNT_ID)
                .header("Idempotency-Key", OPERATION_ID.toString())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(100.00));
  }

  @Test
  void shouldDebitAccount() throws Exception {
    Account account = createAccount();
    account.credit(new BigDecimal("100.00"));
    account.debit(new BigDecimal("25.00"));

    when(accountService.debit(ACCOUNT_ID, new BigDecimal("25.00"), OPERATION_ID))
        .thenReturn(account);

    AmountRequest request = new AmountRequest(new BigDecimal("25.00"));

    mockMvc
        .perform(
            post("/api/accounts/{id}/debit", ACCOUNT_ID)
                .header("Idempotency-Key", OPERATION_ID.toString())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(75.00));
  }

  @Test
  void shouldRejectMissingIdempotencyKey() throws Exception {
    AmountRequest request = new AmountRequest(new BigDecimal("25.00"));

    mockMvc
        .perform(
            post("/api/accounts/{id}/debit", ACCOUNT_ID)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRejectInvalidCreateAccountRequest() throws Exception {
    CreateAccountRequest request = new CreateAccountRequest(null, "", null, "");

    mockMvc
        .perform(
            post("/api/accounts")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRejectInvalidAmount() throws Exception {
    AmountRequest request = new AmountRequest(BigDecimal.ZERO);

    mockMvc
        .perform(
            post("/api/accounts/{id}/credit", ACCOUNT_ID)
                .header("Idempotency-Key", OPERATION_ID.toString())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  private Account createAccount() {
    return Account.create(CUSTOMER_ID, IBAN, AccountType.CHECKING, "EUR");
  }
}
