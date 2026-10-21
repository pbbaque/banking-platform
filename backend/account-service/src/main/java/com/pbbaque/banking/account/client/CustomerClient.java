package com.pbbaque.banking.account.client;

import com.pbbaque.banking.account.config.InternalServiceProperties;
import com.pbbaque.banking.account.dto.CustomerResponse;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** HTTP client used to communicate with customer-service. */
@Component
public class CustomerClient {

  private static final String INTERNAL_SERVICE_TOKEN_HEADER = "X-Internal-Service-Token";

  private final RestClient restClient;
  private final InternalServiceProperties internalServiceProperties;

  /**
   * Creates the customer-service client.
   *
   * @param restClientBuilder REST client builder
   * @param customerServiceBaseUrl customer-service base URL
   * @param internalServiceProperties internal service authentication properties
   */
  public CustomerClient(
      RestClient.Builder restClientBuilder,
      @Value("${customer-service.base-url}") String customerServiceBaseUrl,
      InternalServiceProperties internalServiceProperties) {

    this.restClient = restClientBuilder.baseUrl(customerServiceBaseUrl).build();

    this.internalServiceProperties = internalServiceProperties;
  }

  /**
   * Retrieves the customer associated with an identity user.
   *
   * @param identityUserId identity-service user identifier
   * @return matching customer, or {@code null} when no customer is associated
   */
  public CustomerResponse getByIdentityUserId(UUID identityUserId) {
    try {
      return restClient
          .get()
          .uri("/api/internal/customers/by-identity-user/{identityUserId}", identityUserId)
          .header(INTERNAL_SERVICE_TOKEN_HEADER, internalServiceProperties.token())
          .retrieve()
          .body(CustomerResponse.class);
    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
        return null;
      }

      throw exception;
    }
  }
}
