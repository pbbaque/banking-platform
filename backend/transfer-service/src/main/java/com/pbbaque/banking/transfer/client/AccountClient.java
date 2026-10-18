package com.pbbaque.banking.transfer.client;

import com.pbbaque.banking.transfer.config.AccountServiceProperties;
import com.pbbaque.banking.transfer.exception.AccountServiceException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** HTTP client used to communicate with the account service. */
@Component
public class AccountClient {

  private final RestClient restClient;

  /**
   * Creates the account service HTTP client.
   *
   * @param restClientBuilder Spring REST client builder
   * @param properties account service configuration properties
   */
  public AccountClient(RestClient.Builder restClientBuilder, AccountServiceProperties properties) {
    this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
  }

  /**
   * Debits an amount from an account through the account service.
   *
   * @param accountId account identifier
   * @param amount amount to debit
   * @throws AccountServiceException when the account service rejects the operation or cannot be
   *     reached
   */
  public void debit(UUID accountId, BigDecimal amount) {
    try {
      restClient
          .post()
          .uri("/api/accounts/{id}/debit", accountId)
          .body(Map.of("amount", amount))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException exception) {
      throw new AccountServiceException(
          "Failed to debit account " + accountId, exception.getStatusCode(), exception);
    } catch (RestClientException exception) {
      throw new AccountServiceException("Failed to debit account " + accountId, exception);
    }
  }

  /**
   * Credits an amount to an account through the account service.
   *
   * @param accountId account identifier
   * @param amount amount to credit
   * @throws AccountServiceException when the account service rejects the operation or cannot be
   *     reached
   */
  public void credit(UUID accountId, BigDecimal amount) {
    try {
      restClient
          .post()
          .uri("/api/accounts/{id}/credit", accountId)
          .body(Map.of("amount", amount))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException exception) {
      throw new AccountServiceException(
          "Failed to credit account " + accountId, exception.getStatusCode(), exception);
    } catch (RestClientException exception) {
      throw new AccountServiceException("Failed to credit account " + accountId, exception);
    }
  }
}
