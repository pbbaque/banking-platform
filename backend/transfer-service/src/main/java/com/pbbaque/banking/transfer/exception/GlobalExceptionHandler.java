package com.pbbaque.banking.transfer.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Centralized exception handler for the transfer service API. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handles transfer not found errors.
   *
   * @param exception thrown exception
   * @param request current HTTP request
   * @return standardized API error response
   */
  @ExceptionHandler(TransferNotFoundException.class)
  public ResponseEntity<ApiError> handleTransferNotFound(
      TransferNotFoundException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
  }

  /**
   * Handles idempotency key conflicts.
   *
   * @param exception thrown exception
   * @param request current HTTP request
   * @return standardized API error response
   */
  @ExceptionHandler(IdempotencyConflictException.class)
  public ResponseEntity<ApiError> handleIdempotencyConflict(
      IdempotencyConflictException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
  }

  /**
   * Handles failures while communicating with the account service.
   *
   * <p>If the account service returned an HTTP response, its status code is preserved. If the
   * account service could not be reached or no HTTP response was received, the transfer service
   * returns {@code 502 Bad Gateway}.
   *
   * @param exception thrown exception
   * @param request current HTTP request
   * @return standardized API error response
   */
  @ExceptionHandler(AccountServiceException.class)
  public ResponseEntity<ApiError> handleAccountService(
      AccountServiceException exception, HttpServletRequest request) {

    HttpStatusCode statusCode = exception.getStatusCode();

    if (statusCode == null) {
      return buildResponse(HttpStatus.BAD_GATEWAY, exception.getMessage(), request);
    }

    HttpStatus resolvedStatus = HttpStatus.resolve(statusCode.value());

    String error =
        resolvedStatus != null ? resolvedStatus.getReasonPhrase() : "Account Service Error";

    ApiError apiError =
        new ApiError(
            OffsetDateTime.now(ZoneOffset.UTC),
            statusCode.value(),
            error,
            exception.getMessage(),
            request.getRequestURI());

    return ResponseEntity.status(statusCode).body(apiError);
  }

  /**
   * Handles bean validation errors in request bodies.
   *
   * @param exception thrown exception
   * @param request current HTTP request
   * @return standardized API error response
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {

    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(
                error ->
                    error.getField()
                        + ": "
                        + (error.getDefaultMessage() != null
                            ? error.getDefaultMessage()
                            : "invalid value"))
            .orElse("Request validation failed");

    return buildResponse(HttpStatus.BAD_REQUEST, message, request);
  }

  /**
   * Handles invalid request parameter types.
   *
   * @param exception thrown exception
   * @param request current HTTP request
   * @return standardized API error response
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception, HttpServletRequest request) {

    String message = "Invalid value for parameter: " + exception.getName();

    return buildResponse(HttpStatus.BAD_REQUEST, message, request);
  }

  /**
   * Builds a standardized API error response.
   *
   * @param status HTTP status to return
   * @param message error description
   * @param request current HTTP request
   * @return standardized API error response
   */
  private ResponseEntity<ApiError> buildResponse(
      HttpStatus status, String message, HttpServletRequest request) {

    ApiError error =
        new ApiError(
            OffsetDateTime.now(ZoneOffset.UTC),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI());

    return ResponseEntity.status(status).body(error);
  }
}
