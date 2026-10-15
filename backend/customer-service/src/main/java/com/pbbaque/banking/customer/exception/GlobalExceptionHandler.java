package com.pbbaque.banking.customer.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Centralized exception handler for customer service REST endpoints. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handles attempts to create a customer that already exists.
   *
   * @param exception raised exception
   * @param request current HTTP request
   * @return conflict error response
   */
  @ExceptionHandler(CustomerAlreadyExistsException.class)
  public ResponseEntity<ApiError> handleCustomerAlreadyExistsException(
      CustomerAlreadyExistsException exception, HttpServletRequest request) {
    ApiError error =
        new ApiError(
            OffsetDateTime.now(),
            HttpStatus.CONFLICT.value(),
            HttpStatus.CONFLICT.getReasonPhrase(),
            exception.getMessage(),
            request.getRequestURI());

    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  /**
   * Handles request validation failures.
   *
   * @param exception raised validation exception
   * @param request current HTTP request
   * @return bad request error response
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidationException(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .orElse("Request validation failed");

    ApiError error =
        new ApiError(
            OffsetDateTime.now(ZoneOffset.UTC),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            message,
            request.getRequestURI());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  /**
   * Handles requests for customers that cannot be found.
   *
   * @param exception raised exception
   * @param request current HTTP request
   * @return not found error response
   */
  @ExceptionHandler(CustomerNotFoundException.class)
  public ResponseEntity<ApiError> handleCustomerNotFound(
      CustomerNotFoundException exception, HttpServletRequest request) {
    ApiError error =
        new ApiError(
            OffsetDateTime.now(ZoneOffset.UTC),
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            exception.getMessage(),
            request.getRequestURI());

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  /**
   * Handles invalid customer status transitions.
   *
   * @param exception raised exception
   * @param request current HTTP request
   * @return conflict error response
   */
  @ExceptionHandler(InvalidCustomerStatusTransitionException.class)
  public ResponseEntity<ApiError> handleInvalidStatusTransition(
      InvalidCustomerStatusTransitionException exception, HttpServletRequest request) {
    ApiError error =
        new ApiError(
            OffsetDateTime.now(ZoneOffset.UTC),
            HttpStatus.CONFLICT.value(),
            HttpStatus.CONFLICT.getReasonPhrase(),
            exception.getMessage(),
            request.getRequestURI());

    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  /**
   * Handles invalid request parameter types.
   *
   * @param exception raised type mismatch exception
   * @param request current HTTP request
   * @return bad request error response
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
    ApiError error =
        new ApiError(
            OffsetDateTime.now(ZoneOffset.UTC),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "Invalid value for parameter: " + exception.getName(),
            request.getRequestURI());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }
}
