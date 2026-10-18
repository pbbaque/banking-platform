package com.pbbaque.banking.transfer.exception;

/** Exception thrown when an idempotency key is reused with different transfer data. */
public class IdempotencyConflictException extends RuntimeException {

  /**
   * Creates an idempotency conflict exception.
   *
   * @param idempotencyKey conflicting idempotency key
   */
  public IdempotencyConflictException(String idempotencyKey) {
    super("Idempotency key already used with different transfer data: " + idempotencyKey);
  }
}
