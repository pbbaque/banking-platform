package com.pbbaque.banking.customer.exception;

/** Exception thrown when a requested customer cannot be found. */
public class CustomerNotFoundException extends RuntimeException {

  public CustomerNotFoundException(String message) {
    super(message);
  }
}
