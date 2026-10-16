package com.pbbaque.banking.account.exception;

/** Raised when attempting to create an account that already exists. */
public class AccountAlreadyExistsException extends RuntimeException {

  /**
   * Creates a new account-already-exists exception.
   *
   * @param message detailed error message
   */
  public AccountAlreadyExistsException(String message) {
    super(message);
  }
}
