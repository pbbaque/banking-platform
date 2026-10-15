package com.pbbaque.banking.identity.exception;

/** Exception thrown when authentication fails because the provided credentials are invalid. */
public class InvalidCredentialsException extends RuntimeException {

  public InvalidCredentialsException(String message) {
    super(message);
  }
}
