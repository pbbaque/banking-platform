package com.pbbaque.banking.account.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Handles exceptions raised by the account service REST API. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AccountAlreadyExistsException.class)
  public ResponseEntity<ApiError> handleAccountAlreadyExists(
      AccountAlreadyExistsException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(AccountNotFoundException.class)
  public ResponseEntity<ApiError> handleAccountNotFound(
      AccountNotFoundException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI());
  }

  /**
   * Handles validation errors produced by invalid request payloads.
   *
   * @param exception validation exception
   * @param request current HTTP request
   * @return standardized bad request response
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidationException(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .orElse("Validation failed");

    return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
  }

  /**
   * Handles request parameter type conversion errors.
   *
   * @param exception type mismatch exception
   * @param request current HTTP request
   * @return standardized bad request response
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
    return buildResponse(
        HttpStatus.BAD_REQUEST,
        "Invalid value for parameter: " + exception.getName(),
        request.getRequestURI());
  }

  @ExceptionHandler(AccountNotActiveException.class)
  public ResponseEntity<ApiError> handleAccountNotActive(
      AccountNotActiveException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(InsufficientFundsException.class)
  public ResponseEntity<ApiError> handleInsufficientFunds(
      InsufficientFundsException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(InvalidAmountException.class)
  public ResponseEntity<ApiError> handleInvalidAmount(
      InvalidAmountException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI());
  }

  private ResponseEntity<ApiError> buildResponse(HttpStatus status, String message, String path) {
    ApiError error =
        new ApiError(
            OffsetDateTime.now(ZoneOffset.UTC),
            status.value(),
            status.getReasonPhrase(),
            message,
            path);

    return ResponseEntity.status(status).body(error);
  }

  /**
   * Handles conflicts caused by reusing an idempotency key for a different operation.
   *
   * @param exception idempotency conflict
   * @param request current HTTP request
   * @return conflict error response
   */
  @ExceptionHandler(IdempotencyConflictException.class)
  public ResponseEntity<ApiError> handleIdempotencyConflict(
      IdempotencyConflictException exception, HttpServletRequest request) {

    ApiError error =
        new ApiError(
            OffsetDateTime.now(ZoneOffset.UTC),
            HttpStatus.CONFLICT.value(),
            HttpStatus.CONFLICT.getReasonPhrase(),
            exception.getMessage(),
            request.getRequestURI());

    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }
}
