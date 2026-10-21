package com.pbbaque.banking.account.service;

import com.pbbaque.banking.account.client.CustomerClient;
import com.pbbaque.banking.account.domain.Account;
import com.pbbaque.banking.account.dto.CustomerResponse;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Provides authorization checks for authenticated account users. */
@Service
public class AccountAuthorizationService {

  private final CustomerClient customerClient;

  /**
   * Creates the account authorization service.
   *
   * @param customerClient client used to resolve identity users to customers
   */
  public AccountAuthorizationService(CustomerClient customerClient) {
    this.customerClient = customerClient;
  }

  /**
   * Resolves the customer associated with the authenticated JWT.
   *
   * @param jwt authenticated JWT
   * @return authenticated customer's identifier
   */
  public UUID getAuthenticatedCustomerId(Jwt jwt) {
    UUID identityUserId = parseIdentityUserId(jwt);

    CustomerResponse customer = customerClient.getByIdentityUserId(identityUserId);

    if (customer == null || customer.id() == null) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Authenticated user is not associated with a customer");
    }

    return customer.id();
  }

  /**
   * Ensures that an account belongs to the authenticated customer.
   *
   * @param account account being accessed
   * @param jwt authenticated JWT
   */
  public void requireOwnership(Account account, Jwt jwt) {
    UUID customerId = getAuthenticatedCustomerId(jwt);

    if (!account.getCustomerId().equals(customerId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Account does not belong to the authenticated customer");
    }
  }

  /**
   * Ensures that the supplied customer identifier belongs to the authenticated identity.
   *
   * @param customerId customer identifier being accessed
   * @param jwt authenticated JWT
   */
  public void requireCustomerOwnership(UUID customerId, Jwt jwt) {
    UUID authenticatedCustomerId = getAuthenticatedCustomerId(jwt);

    if (!authenticatedCustomerId.equals(customerId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Customer does not belong to the authenticated identity");
    }
  }

  private UUID parseIdentityUserId(Jwt jwt) {
    String subject = jwt.getSubject();

    if (subject == null) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "JWT subject is not a valid identity user identifier");
    }

    try {
      return UUID.fromString(subject);
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "JWT subject is not a valid identity user identifier", exception);
    }
  }
}
