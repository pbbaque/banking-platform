package com.pbbaque.banking.transfer.exception;

import java.util.UUID;

/** Exception thrown when a requested transfer cannot be found. */
public class TransferNotFoundException extends RuntimeException {

  /**
   * Creates a transfer not found exception using the transfer identifier.
   *
   * @param transferId transfer identifier
   */
  public TransferNotFoundException(UUID transferId) {
    super("Transfer not found: " + transferId);
  }

  /**
   * Creates a transfer not found exception using the idempotency key.
   *
   * @param idempotencyKey idempotency key
   */
  public TransferNotFoundException(String idempotencyKey) {
    super("Transfer not found for idempotency key: " + idempotencyKey);
  }
}
