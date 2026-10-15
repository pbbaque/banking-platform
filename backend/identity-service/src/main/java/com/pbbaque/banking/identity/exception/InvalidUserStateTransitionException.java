package com.pbbaque.banking.identity.exception;

/** Exception thrown when a user attempts an invalid status transition. */
public class InvalidUserStateTransitionException extends RuntimeException {

  public InvalidUserStateTransitionException(String message) {
    super(message);
  }
}
