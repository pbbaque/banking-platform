package com.pbbaque.banking.customer.exception;

/** Exception thrown when attempting to create a customer that already exists. */
public class CustomerAlreadyExistsException extends RuntimeException {

  public CustomerAlreadyExistsException(String message) {
    super(message);
  }
}
