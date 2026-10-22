package com.pbbaque.banking.account.dto;

import com.pbbaque.banking.account.domain.AccountOperation;
import com.pbbaque.banking.account.domain.AccountOperationType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API response representing a completed account balance operation.
 *
 * @param id operation identifier
 * @param accountId affected account identifier
 * @param operationType operation type
 * @param amount operation amount
 * @param resultingBalance account balance immediately after the operation
 * @param createdAt operation execution time
 */
public record AccountOperationResponse(
    UUID id,
    UUID accountId,
    AccountOperationType operationType,
    BigDecimal amount,
    BigDecimal resultingBalance,
    OffsetDateTime createdAt) {

  /**
   * Creates a response from an account operation.
   *
   * @param operation account operation
   * @return operation response
   */
  public static AccountOperationResponse from(AccountOperation operation) {
    return new AccountOperationResponse(
        operation.getId(),
        operation.getAccountId(),
        operation.getOperationType(),
        operation.getAmount(),
        operation.getResultingBalance(),
        operation.getCreatedAt());
  }
}
