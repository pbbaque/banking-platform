package com.pbbaque.banking.identity.exception;

/** Exception thrown when a requested user cannot be found. */
public class UserNotFoundException extends RuntimeException {

  public UserNotFoundException(String message) {
    super(message);
  }
}
