package com.pbbaque.banking.account.exception;

/** Raised when a monetary amount is invalid for an account operation. */
public class InvalidAmountException extends RuntimeException {

  /**
   * Creates a new invalid-amount exception.
   *
   * @param message detailed error message
   */
  public InvalidAmountException(String message) {
    super(message);
  }
}
