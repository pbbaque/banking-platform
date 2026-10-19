package com.pbbaque.banking.transfer.client;

import com.pbbaque.banking.transfer.config.AccountServiceProperties;
import com.pbbaque.banking.transfer.dto.AccountResponse;
import com.pbbaque.banking.transfer.dto.AmountRequest;
import com.pbbaque.banking.transfer.exception.AccountServiceException;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** HTTP client used to communicate with account-service. */
@Component
public class AccountClient {

  private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

  private final RestClient restClient;

  /**
   * Creates a new account-service client.
   *
   * @param restClientBuilder REST client builder
   * @param properties account-service configuration
   */
  public AccountClient(RestClient.Builder restClientBuilder, AccountServiceProperties properties) {
    this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
  }

  /**
   * Retrieves an account.
   *
   * @param accountId account identifier
   * @return account information
   * @throws AccountServiceException when account-service rejects the request or cannot be reached
   */
  public AccountResponse getAccount(UUID accountId) {
    try {
      return restClient
          .get()
          .uri("/api/accounts/{id}", accountId)
          .retrieve()
          .body(AccountResponse.class);
    } catch (RestClientResponseException exception) {
      throw new AccountServiceException(
          "Account service rejected account lookup", exception.getStatusCode(), exception);
    } catch (RestClientException exception) {
      throw new AccountServiceException("Unable to communicate with account service", exception);
    }
  }

  /**
   * Debits funds from an account.
   *
   * <p>The operation identifier is sent to account-service as an idempotency key. Reusing the same
   * identifier for a retry prevents the account balance from being debited more than once.
   *
   * @param accountId account identifier
   * @param amount amount to debit
   * @param operationId stable identifier of the logical debit operation
   * @return updated account
   * @throws AccountServiceException when account-service rejects the request or cannot be reached
   */
  public AccountResponse debit(UUID accountId, BigDecimal amount, UUID operationId) {
    try {
      return restClient
          .post()
          .uri("/api/accounts/{id}/debit", accountId)
          .header(IDEMPOTENCY_KEY_HEADER, operationId.toString())
          .body(new AmountRequest(amount))
          .retrieve()
          .body(AccountResponse.class);
    } catch (RestClientResponseException exception) {
      throw new AccountServiceException(
          "Account service rejected debit operation", exception.getStatusCode(), exception);
    } catch (RestClientException exception) {
      throw new AccountServiceException("Unable to communicate with account service", exception);
    }
  }

  /**
   * Credits funds to an account.
   *
   * <p>The operation identifier is sent to account-service as an idempotency key. Reusing the same
   * identifier for a retry prevents the account balance from being credited more than once.
   *
   * @param accountId account identifier
   * @param amount amount to credit
   * @param operationId stable identifier of the logical credit operation
   * @return updated account
   * @throws AccountServiceException when account-service rejects the request or cannot be reached
   */
  public AccountResponse credit(UUID accountId, BigDecimal amount, UUID operationId) {
    try {
      return restClient
          .post()
          .uri("/api/accounts/{id}/credit", accountId)
          .header(IDEMPOTENCY_KEY_HEADER, operationId.toString())
          .body(new AmountRequest(amount))
          .retrieve()
          .body(AccountResponse.class);
    } catch (RestClientResponseException exception) {
      throw new AccountServiceException(
          "Account service rejected credit operation", exception.getStatusCode(), exception);
    } catch (RestClientException exception) {
      throw new AccountServiceException("Unable to communicate with account service", exception);
    }
  }
}
