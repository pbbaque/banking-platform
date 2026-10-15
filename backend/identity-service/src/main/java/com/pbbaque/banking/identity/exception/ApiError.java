package com.pbbaque.banking.identity.exception;

import java.time.OffsetDateTime;

/**
 * Standard error response returned by the identity service API.
 *
 * @param timestamp time when the error occurred
 * @param status HTTP status code
 * @param error HTTP error description
 * @param message detailed error message
 * @param path request path that produced the error
 */
public record ApiError(
    OffsetDateTime timestamp, int status, String error, String message, String path) {}
