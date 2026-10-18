package com.pbbaque.banking.transfer.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.pbbaque.banking.transfer.config.AccountServiceProperties;
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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/** Unit tests for {@link AccountClient}. */
class AccountClientTest {

  private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  private static final BigDecimal AMOUNT = new BigDecimal("100.0000");

  private MockRestServiceServer server;
  private AccountClient accountClient;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();

    server = MockRestServiceServer.bindTo(builder).build();

    AccountServiceProperties properties = new AccountServiceProperties("http://localhost:8083");

    accountClient = new AccountClient(builder, properties);
  }

  @Test
  void shouldDebitAccount() {
    server
        .expect(requestTo("http://localhost:8083/api/accounts/" + ACCOUNT_ID + "/debit"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("{\"amount\":100.0000}"))
        .andRespond(withSuccess());

    accountClient.debit(ACCOUNT_ID, AMOUNT);

    server.verify();
  }

  @Test
  void shouldCreditAccount() {
    server
        .expect(requestTo("http://localhost:8083/api/accounts/" + ACCOUNT_ID + "/credit"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("{\"amount\":100.0000}"))
        .andRespond(withSuccess());

    accountClient.credit(ACCOUNT_ID, AMOUNT);

    server.verify();
  }

  @Test
  void shouldPreserveDebitHttpFailureStatus() {
    server
        .expect(requestTo("http://localhost:8083/api/accounts/" + ACCOUNT_ID + "/debit"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withBadRequest());

    AccountServiceException exception =
        assertThrows(AccountServiceException.class, () -> accountClient.debit(ACCOUNT_ID, AMOUNT));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

    server.verify();
  }

  @Test
  void shouldPreserveCreditHttpFailureStatus() {
    server
        .expect(requestTo("http://localhost:8083/api/accounts/" + ACCOUNT_ID + "/credit"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withBadRequest());

    AccountServiceException exception =
        assertThrows(AccountServiceException.class, () -> accountClient.credit(ACCOUNT_ID, AMOUNT));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

    server.verify();
  }

  @Test
  void shouldWrapDebitCommunicationFailureWithoutStatus() {
    server
        .expect(requestTo("http://localhost:8083/api/accounts/" + ACCOUNT_ID + "/debit"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            request -> {
              throw new ResourceAccessException(
                  "Connection refused", new IOException("Connection refused"));
            });

    AccountServiceException exception =
        assertThrows(AccountServiceException.class, () -> accountClient.debit(ACCOUNT_ID, AMOUNT));

    assertNull(exception.getStatusCode());

    server.verify();
  }

  @Test
  void shouldWrapCreditCommunicationFailureWithoutStatus() {
    server
        .expect(requestTo("http://localhost:8083/api/accounts/" + ACCOUNT_ID + "/credit"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            request -> {
              throw new ResourceAccessException(
                  "Connection refused", new IOException("Connection refused"));
            });

    AccountServiceException exception =
        assertThrows(AccountServiceException.class, () -> accountClient.credit(ACCOUNT_ID, AMOUNT));

    assertNull(exception.getStatusCode());

    server.verify();
  }
}
