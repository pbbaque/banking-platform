package com.pbbaque.banking.account.dto;

import com.pbbaque.banking.account.domain.Account;
import com.pbbaque.banking.account.domain.AccountStatus;
import com.pbbaque.banking.account.domain.AccountType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Response payload returned for banking account operations. */
public record AccountResponse(
    UUID id,
    UUID customerId,
    String iban,
    AccountType accountType,
    String currency,
    BigDecimal balance,
    AccountStatus status,
    Long version,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    OffsetDateTime closedAt) {

  /**
   * Creates an API response from an account entity.
   *
   * @param account account entity
   * @return account response
   */
  public static AccountResponse from(Account account) {
    return new AccountResponse(
        account.getId(),
        account.getCustomerId(),
        account.getIban(),
        account.getAccountType(),
        account.getCurrency(),
        account.getBalance(),
        account.getStatus(),
        account.getVersion(),
        account.getCreatedAt(),
        account.getUpdatedAt(),
        account.getClosedAt());
  }
}
