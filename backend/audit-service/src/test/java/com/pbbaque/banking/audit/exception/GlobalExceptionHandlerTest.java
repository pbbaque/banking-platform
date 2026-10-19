package com.pbbaque.banking.audit.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Unit tests for {@link GlobalExceptionHandler}. */
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler globalExceptionHandler;
  private HttpServletRequest request;

  @BeforeEach
  void setUp() {
    globalExceptionHandler = new GlobalExceptionHandler();

    request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getRequestURI()).thenReturn("/api/audit-events");
  }

  @Test
  void shouldReturnNotFoundForMissingAuditEvent() {
    UUID id = UUID.randomUUID();

    ResponseEntity<ApiError> response =
        globalExceptionHandler.handleAuditEventNotFound(
            new AuditEventNotFoundException(id), request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(404, response.getBody().status());
    assertEquals("Not Found", response.getBody().error());
    assertEquals("Audit event not found: " + id, response.getBody().message());
    assertEquals("/api/audit-events", response.getBody().path());
    assertNotNull(response.getBody().timestamp());
  }

  @Test
  void shouldReturnBadRequestForValidationErrors() throws Exception {
    TestRequest target = new TestRequest();

    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "testRequest");

    bindingResult.addError(new FieldError("testRequest", "eventType", "must not be blank"));

    MethodParameter parameter =
        new MethodParameter(
            GlobalExceptionHandlerTest.class.getDeclaredMethod(
                "validationTarget", TestRequest.class),
            0);

    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(parameter, bindingResult);

    ResponseEntity<ApiError> response = globalExceptionHandler.handleValidation(exception, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(400, response.getBody().status());
    assertEquals("Bad Request", response.getBody().error());
    assertEquals("eventType: must not be blank", response.getBody().message());
    assertEquals("/api/audit-events", response.getBody().path());
  }

  @Test
  void shouldHandleValidationErrorWithoutDefaultMessage() throws Exception {
    TestRequest target = new TestRequest();

    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "testRequest");

    bindingResult.addError(new FieldError("testRequest", "eventType", null));

    MethodParameter parameter =
        new MethodParameter(
            GlobalExceptionHandlerTest.class.getDeclaredMethod(
                "validationTarget", TestRequest.class),
            0);

    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(parameter, bindingResult);

    ResponseEntity<ApiError> response = globalExceptionHandler.handleValidation(exception, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("eventType is invalid", response.getBody().message());
  }

  @Test
  void shouldReturnBadRequestForTypeMismatch() {
    MethodArgumentTypeMismatchException exception =
        new MethodArgumentTypeMismatchException(
            "invalid-uuid",
            UUID.class,
            "actorId",
            null,
            new IllegalArgumentException("Invalid UUID"));

    ResponseEntity<ApiError> response =
        globalExceptionHandler.handleTypeMismatch(exception, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(400, response.getBody().status());
    assertEquals("Bad Request", response.getBody().error());
    assertEquals("Invalid value for parameter: actorId", response.getBody().message());
    assertEquals("/api/audit-events", response.getBody().path());
  }

  @SuppressWarnings("unused")
  private void validationTarget(TestRequest request) {}

  private static class TestRequest {}
}
