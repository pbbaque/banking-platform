package com.pbbaque.banking.transfer.service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Generates stable identifiers for account operations belonging to a transfer.
 *
 * <p>The same transfer identifier and operation type always produce the same UUID, allowing remote
 * account operations to be retried safely after timeouts or service restarts.
 */
@Component
public class AccountOperationIdFactory {

  /**
   * Generates the identifier for the source-account debit.
   *
   * @param transferId transfer identifier
   * @return stable debit operation identifier
   */
  public UUID debit(UUID transferId) {
    return generate(transferId, "DEBIT");
  }

  /**
   * Generates the identifier for the destination-account credit.
   *
   * @param transferId transfer identifier
   * @return stable credit operation identifier
   */
  public UUID credit(UUID transferId) {
    return generate(transferId, "CREDIT");
  }

  /**
   * Generates the identifier for a compensating credit to the source account.
   *
   * @param transferId transfer identifier
   * @return stable compensation operation identifier
   */
  public UUID compensation(UUID transferId) {
    return generate(transferId, "COMPENSATION");
  }

  private UUID generate(UUID transferId, String operationType) {
    String value = transferId + ":" + operationType;

    return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
  }
}
