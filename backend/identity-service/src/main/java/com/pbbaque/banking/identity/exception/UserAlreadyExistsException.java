package com.pbbaque.banking.identity.exception;

/** Exception thrown when attempting to create a user that already exists. */
public class UserAlreadyExistsException extends RuntimeException {

  public UserAlreadyExistsException(String message) {
    super(message);
  }
}
