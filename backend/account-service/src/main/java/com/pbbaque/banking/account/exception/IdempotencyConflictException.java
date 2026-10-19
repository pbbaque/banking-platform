package com.pbbaque.banking.account.exception;

/** Thrown when an idempotency key is reused for a different account balance operation. */
public class IdempotencyConflictException extends RuntimeException {

  /**
   * Creates a new idempotency conflict exception.
   *
   * @param message exception message
   */
  public IdempotencyConflictException(String message) {
    super(message);
  }
}
