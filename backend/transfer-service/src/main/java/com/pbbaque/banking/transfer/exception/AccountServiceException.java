package com.pbbaque.banking.transfer.exception;

import org.springframework.http.HttpStatusCode;

/**
 * Exception thrown when an operation against the account service cannot be completed.
 *
 * <p>If the account service returned an HTTP response, the original HTTP status is preserved so
 * that the transfer service can distinguish a business rejection from an infrastructure failure.
 */
public class AccountServiceException extends RuntimeException {

  private final HttpStatusCode statusCode;

  /**
   * Creates an account service exception for an HTTP error response returned by the account
   * service.
   *
   * @param message error description
   * @param statusCode HTTP status returned by the account service
   * @param cause original exception
   */
  public AccountServiceException(String message, HttpStatusCode statusCode, Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
  }

  /**
   * Creates an account service exception for a communication failure where no HTTP response was
   * received.
   *
   * @param message error description
   * @param cause original exception
   */
  public AccountServiceException(String message, Throwable cause) {
    super(message, cause);
    this.statusCode = null;
  }

  /**
   * Returns the HTTP status returned by the account service.
   *
   * @return HTTP status, or {@code null} when no HTTP response was received
   */
  public HttpStatusCode getStatusCode() {
    return statusCode;
  }
}
