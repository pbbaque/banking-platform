package com.pbbaque.banking.account.exception;

/** Raised when a requested account cannot be found. */
public class AccountNotFoundException extends RuntimeException {

  /**
   * Creates a new account-not-found exception.
   *
   * @param message detailed error message
   */
  public AccountNotFoundException(String message) {
    super(message);
  }
}
