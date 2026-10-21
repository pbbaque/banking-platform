package com.pbbaque.banking.account.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbbaque.banking.account.config.SecurityConfig;
import com.pbbaque.banking.account.domain.Account;
import com.pbbaque.banking.account.domain.AccountType;
import com.pbbaque.banking.account.dto.CreateAccountRequest;
import com.pbbaque.banking.account.service.AccountAuthorizationService;
import com.pbbaque.banking.account.service.AccountService;
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

/** Web layer tests for {@link AccountController}. */
@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
class AccountControllerTest {

  private static final UUID ACCOUNT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  private static final UUID IDENTITY_USER_ID =
      UUID.fromString("44444444-4444-4444-4444-444444444444");

  private static final String IBAN = "ES9121000418450200051332";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AccountService accountService;

  @MockitoBean private AccountAuthorizationService accountAuthorizationService;

  @MockitoBean private JwtDecoder jwtDecoder;

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
                .with(userJwt())
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
        .perform(get("/api/accounts/{id}", ACCOUNT_ID).with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.iban").value(IBAN))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void shouldGetAccountByIban() throws Exception {
    when(accountService.getByIban(IBAN)).thenReturn(createAccount());

    mockMvc
        .perform(get("/api/accounts/iban/{iban}", IBAN).with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.iban").value(IBAN));
  }

  @Test
  void shouldGetAccountsByCustomerId() throws Exception {
    when(accountService.getByCustomerId(CUSTOMER_ID)).thenReturn(List.of(createAccount()));

    mockMvc
        .perform(get("/api/accounts").with(userJwt()).param("customerId", CUSTOMER_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].customerId").value(CUSTOMER_ID.toString()))
        .andExpect(jsonPath("$[0].iban").value(IBAN));
  }

  @Test
  void shouldBlockAccount() throws Exception {
    Account account = createAccount();
    Account blockedAccount = createAccount();
    blockedAccount.block();

    when(accountService.getById(ACCOUNT_ID)).thenReturn(account);
    when(accountService.block(ACCOUNT_ID)).thenReturn(blockedAccount);

    mockMvc
        .perform(put("/api/accounts/{id}/block", ACCOUNT_ID).with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("BLOCKED"));
  }

  @Test
  void shouldActivateAccount() throws Exception {
    Account account = createAccount();

    when(accountService.getById(ACCOUNT_ID)).thenReturn(account);
    when(accountService.activate(ACCOUNT_ID)).thenReturn(account);

    mockMvc
        .perform(put("/api/accounts/{id}/activate", ACCOUNT_ID).with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void shouldCloseAccount() throws Exception {
    Account account = createAccount();
    Account closedAccount = createAccount();
    closedAccount.close();

    when(accountService.getById(ACCOUNT_ID)).thenReturn(account);
    when(accountService.close(ACCOUNT_ID)).thenReturn(closedAccount);

    mockMvc
        .perform(put("/api/accounts/{id}/close", ACCOUNT_ID).with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"));
  }

  @Test
  void shouldRejectInvalidCreateAccountRequest() throws Exception {
    CreateAccountRequest request = new CreateAccountRequest(null, "", null, "");

    mockMvc
        .perform(
            post("/api/accounts")
                .with(userJwt())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRejectProtectedEndpointWithoutJwt() throws Exception {
    mockMvc.perform(get("/api/accounts/{id}", ACCOUNT_ID)).andExpect(status().isUnauthorized());
  }

  private Account createAccount() {
    return Account.create(CUSTOMER_ID, IBAN, AccountType.CHECKING, "EUR");
  }

  private JwtRequestPostProcessor userJwt() {
    return jwt()
        .jwt(token -> token.subject(IDENTITY_USER_ID.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }
}
