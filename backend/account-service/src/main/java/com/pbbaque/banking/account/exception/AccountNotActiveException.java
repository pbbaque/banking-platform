package com.pbbaque.banking.account.exception;

/** Raised when an operation requires an active account. */
public class AccountNotActiveException extends RuntimeException {

  /**
   * Creates a new account-not-active exception.
   *
   * @param message detailed error message
   */
  public AccountNotActiveException(String message) {
    super(message);
  }
}
