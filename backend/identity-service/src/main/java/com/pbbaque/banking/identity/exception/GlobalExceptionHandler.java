package com.pbbaque.banking.identity.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Handles exceptions raised by the identity service REST API. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<ApiError> handleUserAlreadyExists(
      UserAlreadyExistsException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ApiError> handleUserNotFound(
      UserNotFoundException exception, HttpServletRequest request) {
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

  @ExceptionHandler(InvalidUserStateTransitionException.class)
  public ResponseEntity<ApiError> handleInvalidUserStateTransition(
      InvalidUserStateTransitionException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ApiError> handleInvalidCredentials(
      InvalidCredentialsException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), request.getRequestURI());
  }
}
