package com.pbbaque.banking.transfer.exception;

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

  private GlobalExceptionHandler handler;
  private HttpServletRequest request;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
    request = Mockito.mock(HttpServletRequest.class);

    Mockito.when(request.getRequestURI()).thenReturn("/api/transfers/test");
  }

  @Test
  void shouldHandleTransferNotFoundException() {
    UUID transferId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    TransferNotFoundException exception = new TransferNotFoundException(transferId);

    ResponseEntity<ApiError> response = handler.handleTransferNotFound(exception, request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    ApiError body = response.getBody();

    assertNotNull(body);
    assertEquals(404, body.status());
    assertEquals("Not Found", body.error());
    assertEquals("Transfer not found: " + transferId, body.message());
    assertEquals("/api/transfers/test", body.path());
    assertNotNull(body.timestamp());
  }

  @Test
  void shouldHandleIdempotencyConflictException() {
    IdempotencyConflictException exception = new IdempotencyConflictException("transfer-key-1");

    ResponseEntity<ApiError> response = handler.handleIdempotencyConflict(exception, request);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

    ApiError body = response.getBody();

    assertNotNull(body);
    assertEquals(409, body.status());
    assertEquals("Conflict", body.error());
    assertEquals(
        "Idempotency key already used with different transfer data: transfer-key-1",
        body.message());
    assertEquals("/api/transfers/test", body.path());
  }

  @Test
  void shouldPropagateAccountServiceHttpStatus() {
    AccountServiceException exception =
        new AccountServiceException(
            "Account operation rejected",
            HttpStatus.CONFLICT,
            new RuntimeException("Remote conflict"));

    ResponseEntity<ApiError> response = handler.handleAccountService(exception, request);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

    ApiError body = response.getBody();

    assertNotNull(body);
    assertEquals(409, body.status());
    assertEquals("Conflict", body.error());
    assertEquals("Account operation rejected", body.message());
    assertEquals("/api/transfers/test", body.path());
  }

  @Test
  void shouldReturnBadGatewayForAccountServiceCommunicationFailure() {
    AccountServiceException exception =
        new AccountServiceException(
            "Account service unavailable", new RuntimeException("Connection refused"));

    ResponseEntity<ApiError> response = handler.handleAccountService(exception, request);

    assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());

    ApiError body = response.getBody();

    assertNotNull(body);
    assertEquals(502, body.status());
    assertEquals("Bad Gateway", body.error());
    assertEquals("Account service unavailable", body.message());
    assertEquals("/api/transfers/test", body.path());
  }

  @Test
  void shouldHandleValidationException() throws Exception {
    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new Object(), "request");

    bindingResult.addError(
        new FieldError("request", "amount", "must be greater than or equal to 0.0001"));

    MethodParameter methodParameter =
        new MethodParameter(
            GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyMethod", String.class), 0);

    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(methodParameter, bindingResult);

    ResponseEntity<ApiError> response = handler.handleValidation(exception, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    ApiError body = response.getBody();

    assertNotNull(body);
    assertEquals(400, body.status());
    assertEquals("Bad Request", body.error());
    assertEquals("amount: must be greater than or equal to 0.0001", body.message());
    assertEquals("/api/transfers/test", body.path());
  }

  @Test
  void shouldHandleTypeMismatchException() throws Exception {
    MethodParameter methodParameter =
        new MethodParameter(
            GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyMethod", String.class), 0);

    MethodArgumentTypeMismatchException exception =
        new MethodArgumentTypeMismatchException(
            "invalid",
            UUID.class,
            "id",
            methodParameter,
            new IllegalArgumentException("Invalid UUID"));

    ResponseEntity<ApiError> response = handler.handleTypeMismatch(exception, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    ApiError body = response.getBody();

    assertNotNull(body);
    assertEquals(400, body.status());
    assertEquals("Bad Request", body.error());
    assertEquals("Invalid value for parameter: id", body.message());
    assertEquals("/api/transfers/test", body.path());
  }

  @SuppressWarnings("unused")
  private void dummyMethod(String value) {}
}
