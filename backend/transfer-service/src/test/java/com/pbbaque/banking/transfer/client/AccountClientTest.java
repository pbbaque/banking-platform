package com.pbbaque.banking.transfer.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.pbbaque.banking.transfer.config.AccountServiceProperties;
import com.pbbaque.banking.transfer.dto.AccountResponse;
import com.pbbaque.banking.transfer.exception.AccountServiceException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Unit tests for {@link AccountClient}. */
class AccountClientTest {

  private static final String ACCOUNT_SERVICE_URL = "http://localhost:8083";

  private static final UUID ACCOUNT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  private static final UUID OPERATION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  private RestClient.Builder restClientBuilder;

  private MockRestServiceServer server;

  private AccountClient accountClient;

  @BeforeEach
  void setUp() {
    restClientBuilder = RestClient.builder();

    server = MockRestServiceServer.bindTo(restClientBuilder).build();

    AccountServiceProperties properties = new AccountServiceProperties(ACCOUNT_SERVICE_URL);

    accountClient = new AccountClient(restClientBuilder, properties);
  }

  @Test
  void shouldGetAccount() {
    server
        .expect(requestTo(ACCOUNT_SERVICE_URL + "/api/accounts/" + ACCOUNT_ID))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(accountJson("100.00"), MediaType.APPLICATION_JSON));

    AccountResponse result = accountClient.getAccount(ACCOUNT_ID);

    assertEquals(ACCOUNT_ID, result.id());
    assertEquals(CUSTOMER_ID, result.customerId());
    assertEquals("EUR", result.currency());
    assertEquals(new BigDecimal("100.00"), result.balance());
    assertEquals("ACTIVE", result.status());

    server.verify();
  }

  @Test
  void shouldDebitAccountWithIdempotencyKey() {
    server
        .expect(requestTo(ACCOUNT_SERVICE_URL + "/api/accounts/" + ACCOUNT_ID + "/debit"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Idempotency-Key", OPERATION_ID.toString()))
        .andExpect(
            content()
                .json(
                    """
                    {
                      "amount": 25.00
                    }
                    """))
        .andRespond(withSuccess(accountJson("75.00"), MediaType.APPLICATION_JSON));

    AccountResponse result = accountClient.debit(ACCOUNT_ID, new BigDecimal("25.00"), OPERATION_ID);

    assertEquals(ACCOUNT_ID, result.id());
    assertEquals(new BigDecimal("75.00"), result.balance());

    server.verify();
  }

  @Test
  void shouldCreditAccountWithIdempotencyKey() {
    server
        .expect(requestTo(ACCOUNT_SERVICE_URL + "/api/accounts/" + ACCOUNT_ID + "/credit"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Idempotency-Key", OPERATION_ID.toString()))
        .andExpect(
            content()
                .json(
                    """
                    {
                      "amount": 25.00
                    }
                    """))
        .andRespond(withSuccess(accountJson("125.00"), MediaType.APPLICATION_JSON));

    AccountResponse result =
        accountClient.credit(ACCOUNT_ID, new BigDecimal("25.00"), OPERATION_ID);

    assertEquals(ACCOUNT_ID, result.id());
    assertEquals(new BigDecimal("125.00"), result.balance());

    server.verify();
  }

  @Test
  void shouldWrapAccountServiceHttpError() {
    server
        .expect(requestTo(ACCOUNT_SERVICE_URL + "/api/accounts/" + ACCOUNT_ID + "/credit"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Idempotency-Key", OPERATION_ID.toString()))
        .andRespond(
            withStatus(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    """
                    {
                      "message": "Account is not active"
                    }
                    """));

    AccountServiceException exception =
        assertThrows(
            AccountServiceException.class,
            () -> accountClient.credit(ACCOUNT_ID, new BigDecimal("25.00"), OPERATION_ID));

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    assertEquals("Account service rejected credit operation", exception.getMessage());

    server.verify();
  }

  @Test
  void shouldWrapAccountServiceCommunicationError() {
    server
        .expect(requestTo(ACCOUNT_SERVICE_URL + "/api/accounts/" + ACCOUNT_ID + "/debit"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Idempotency-Key", OPERATION_ID.toString()))
        .andRespond(withException(new IOException("Connection reset")));

    AccountServiceException exception =
        assertThrows(
            AccountServiceException.class,
            () -> accountClient.debit(ACCOUNT_ID, new BigDecimal("25.00"), OPERATION_ID));

    assertEquals("Unable to communicate with account service", exception.getMessage());
    assertNull(exception.getStatusCode());

    server.verify();
  }

  private String accountJson(String balance) {
    return """
    {
      "id": "%s",
      "customerId": "%s",
      "iban": "ES9121000418450200051332",
      "accountType": "CHECKING",
      "currency": "EUR",
      "balance": %s,
      "status": "ACTIVE",
      "version": 1,
      "createdAt": "2026-10-19T10:00:00Z",
      "updatedAt": "2026-10-19T10:00:00Z",
      "closedAt": null
    }
    """
        .formatted(ACCOUNT_ID, CUSTOMER_ID, balance);
  }
}
