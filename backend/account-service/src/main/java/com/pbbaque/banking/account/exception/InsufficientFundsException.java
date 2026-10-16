package com.pbbaque.banking.account.exception;

/** Raised when an account does not have enough balance to complete an operation. */
public class InsufficientFundsException extends RuntimeException {

  /**
   * Creates a new insufficient-funds exception.
   *
   * @param message detailed error message
   */
  public InsufficientFundsException(String message) {
    super(message);
  }
}
