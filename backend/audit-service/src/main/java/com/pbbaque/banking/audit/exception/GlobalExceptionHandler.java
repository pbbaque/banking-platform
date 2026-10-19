package com.pbbaque.banking.audit.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Global exception handler for the audit service REST API. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handles audit event not found errors.
   *
   * @param exception thrown exception
   * @param request HTTP request
   * @return API error response
   */
  @ExceptionHandler(AuditEventNotFoundException.class)
  public ResponseEntity<ApiError> handleAuditEventNotFound(
      AuditEventNotFoundException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
  }

  /**
   * Handles request validation errors.
   *
   * @param exception validation exception
   * @param request HTTP request
   * @return API error response
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {

    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .map(GlobalExceptionHandler::formatFieldError)
            .collect(Collectors.joining("; "));

    return buildResponse(HttpStatus.BAD_REQUEST, message, request);
  }

  /**
   * Handles invalid request parameter types.
   *
   * @param exception type mismatch exception
   * @param request HTTP request
   * @return API error response
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception, HttpServletRequest request) {

    String message = "Invalid value for parameter: " + exception.getName();

    return buildResponse(HttpStatus.BAD_REQUEST, message, request);
  }

  private static String formatFieldError(FieldError fieldError) {
    String defaultMessage = fieldError.getDefaultMessage();

    if (defaultMessage == null || defaultMessage.isBlank()) {
      return fieldError.getField() + " is invalid";
    }

    return fieldError.getField() + ": " + defaultMessage;
  }

  private ResponseEntity<ApiError> buildResponse(
      HttpStatus status, String message, HttpServletRequest request) {

    ApiError apiError =
        new ApiError(
            OffsetDateTime.now(ZoneOffset.UTC),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI());

    return ResponseEntity.status(status).body(apiError);
  }
}
