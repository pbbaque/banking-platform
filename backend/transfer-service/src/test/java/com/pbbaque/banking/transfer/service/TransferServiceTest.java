package com.pbbaque.banking.transfer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbbaque.banking.transfer.client.AccountClient;
import com.pbbaque.banking.transfer.domain.OutboxEvent;
import com.pbbaque.banking.transfer.domain.OutboxEventStatus;
import com.pbbaque.banking.transfer.domain.Transfer;
import com.pbbaque.banking.transfer.domain.TransferStatus;
import com.pbbaque.banking.transfer.dto.AuditEventMessage;
import com.pbbaque.banking.transfer.dto.CreateTransferRequest;
import com.pbbaque.banking.transfer.exception.AccountServiceException;
import com.pbbaque.banking.transfer.exception.IdempotencyConflictException;
import com.pbbaque.banking.transfer.exception.TransferNotFoundException;
import com.pbbaque.banking.transfer.repository.OutboxEventRepository;
import com.pbbaque.banking.transfer.repository.TransferRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** Unit tests for {@link TransferService}. */
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

  private static final UUID SOURCE_ACCOUNT_ID =
      UUID.fromString("11111111-1111-1111-1111-111111111111");

  private static final UUID DESTINATION_ACCOUNT_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");

  private static final UUID TRANSFER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  private static final BigDecimal AMOUNT = new BigDecimal("100.0000");

  private static final String IDEMPOTENCY_KEY = "transfer-key-1";

  @Mock private TransferRepository transferRepository;

  @Mock private OutboxEventRepository outboxEventRepository;

  @Mock private AccountClient accountClient;

  @Mock private PlatformTransactionManager transactionManager;

  private ObjectMapper objectMapper;

  private TransferService transferService;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();

    transferService =
        new TransferService(
            transferRepository,
            outboxEventRepository,
            accountClient,
            objectMapper,
            transactionManager);
  }

  @Test
  void shouldCreateCompletedTransferAndOutboxEvent() throws Exception {
    CreateTransferRequest request = createRequest();

    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());

    stubTransferPersistence();
    stubOutboxPersistence();
    stubTransaction();

    doNothing().when(accountClient).debit(SOURCE_ACCOUNT_ID, AMOUNT);
    doNothing().when(accountClient).credit(DESTINATION_ACCOUNT_ID, AMOUNT);

    Transfer result = transferService.createTransfer(request);

    assertEquals(TransferStatus.COMPLETED, result.getStatus());

    verify(accountClient).debit(SOURCE_ACCOUNT_ID, AMOUNT);
    verify(accountClient).credit(DESTINATION_ACCOUNT_ID, AMOUNT);

    verify(transferRepository, times(2)).save(any(Transfer.class));

    OutboxEvent outboxEvent = captureOutboxEvent();

    assertEquals(TRANSFER_ID, result.getId());
    assertEquals(result.getId(), outboxEvent.getAggregateId());
    assertEquals("TRANSFER_COMPLETED", outboxEvent.getEventType());
    assertEquals(OutboxEventStatus.PENDING, outboxEvent.getStatus());
    assertEquals(0, outboxEvent.getAttempts());
    assertNull(outboxEvent.getLastError());
    assertNull(outboxEvent.getPublishedAt());
    assertNotNull(outboxEvent.getCreatedAt());
    assertEquals(outboxEvent.getCreatedAt(), outboxEvent.getAvailableAt());

    AuditEventMessage message =
        objectMapper.readValue(outboxEvent.getPayload(), AuditEventMessage.class);

    assertEquals(outboxEvent.getId(), message.eventId());
    assertEquals("TRANSFER_COMPLETED", message.eventType());
    assertEquals("TRANSFER", message.aggregateType());
    assertEquals(TRANSFER_ID.toString(), message.aggregateId());
    assertEquals("SUCCESS", message.outcome());
    assertEquals("Transfer completed successfully", message.description());
    assertNull(message.actorId());
    assertNotNull(message.occurredAt());

    assertEquals(SOURCE_ACCOUNT_ID.toString(), message.metadata().get("sourceAccountId"));
    assertEquals(DESTINATION_ACCOUNT_ID.toString(), message.metadata().get("destinationAccountId"));
    assertEquals("EUR", message.metadata().get("currency"));
    assertEquals(IDEMPOTENCY_KEY, message.metadata().get("idempotencyKey"));
    assertEquals("COMPLETED", message.metadata().get("status"));
  }

  @Test
  void shouldReturnExistingTransferForSameIdempotencyKeyWithoutCreatingOutboxEvent() {
    CreateTransferRequest request = createRequest();

    Transfer existingTransfer =
        Transfer.create(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, AMOUNT, "EUR", IDEMPOTENCY_KEY);

    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
        .thenReturn(Optional.of(existingTransfer));

    Transfer result = transferService.createTransfer(request);

    assertSame(existingTransfer, result);

    verify(transferRepository, never()).save(any(Transfer.class));
    verify(outboxEventRepository, never()).save(any(OutboxEvent.class));

    verify(accountClient, never()).debit(any(), any());
    verify(accountClient, never()).credit(any(), any());
  }

  @Test
  void shouldFailTransferAndCreateOutboxEventWhenDebitFails() throws Exception {
    CreateTransferRequest request = createRequest();

    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());

    stubTransferPersistence();
    stubOutboxPersistence();
    stubTransaction();

    doThrow(
            new AccountServiceException(
                "Debit failed",
                HttpStatus.CONFLICT,
                new RuntimeException("Account service rejected debit")))
        .when(accountClient)
        .debit(SOURCE_ACCOUNT_ID, AMOUNT);

    assertThrows(AccountServiceException.class, () -> transferService.createTransfer(request));

    verify(accountClient).debit(SOURCE_ACCOUNT_ID, AMOUNT);
    verify(accountClient, never()).credit(DESTINATION_ACCOUNT_ID, AMOUNT);

    verify(transferRepository, times(2)).save(any(Transfer.class));

    OutboxEvent outboxEvent = captureOutboxEvent();

    assertEquals(TRANSFER_ID, outboxEvent.getAggregateId());
    assertEquals("TRANSFER_FAILED", outboxEvent.getEventType());
    assertEquals(OutboxEventStatus.PENDING, outboxEvent.getStatus());

    AuditEventMessage message =
        objectMapper.readValue(outboxEvent.getPayload(), AuditEventMessage.class);

    assertEquals("FAILURE", message.outcome());
    assertEquals(
        "Transfer failed because the source account could not be debited", message.description());
    assertEquals("FAILED", message.metadata().get("status"));
  }

  @Test
  void shouldCompensateSourceAccountAndCreateOutboxEventWhenDestinationCreditFails()
      throws Exception {
    CreateTransferRequest request = createRequest();

    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());

    stubTransferPersistence();
    stubOutboxPersistence();
    stubTransaction();

    doNothing().when(accountClient).debit(SOURCE_ACCOUNT_ID, AMOUNT);

    doThrow(
            new AccountServiceException(
                "Destination credit failed",
                HttpStatus.CONFLICT,
                new RuntimeException("Account service rejected destination credit")))
        .when(accountClient)
        .credit(DESTINATION_ACCOUNT_ID, AMOUNT);

    doNothing().when(accountClient).credit(SOURCE_ACCOUNT_ID, AMOUNT);

    assertThrows(AccountServiceException.class, () -> transferService.createTransfer(request));

    verify(accountClient).debit(SOURCE_ACCOUNT_ID, AMOUNT);
    verify(accountClient).credit(DESTINATION_ACCOUNT_ID, AMOUNT);
    verify(accountClient).credit(SOURCE_ACCOUNT_ID, AMOUNT);

    verify(transferRepository, times(2)).save(any(Transfer.class));

    OutboxEvent outboxEvent = captureOutboxEvent();

    assertEquals(TRANSFER_ID, outboxEvent.getAggregateId());
    assertEquals("TRANSFER_FAILED", outboxEvent.getEventType());
    assertEquals(OutboxEventStatus.PENDING, outboxEvent.getStatus());

    AuditEventMessage message =
        objectMapper.readValue(outboxEvent.getPayload(), AuditEventMessage.class);

    assertEquals("FAILURE", message.outcome());
    assertEquals(
        "Transfer failed because the destination account could not be credited; "
            + "the source debit was compensated",
        message.description());
    assertEquals("FAILED", message.metadata().get("status"));
  }

  @Test
  void shouldCreateOutboxEventWhenDestinationCreditAndCompensationFail() throws Exception {
    CreateTransferRequest request = createRequest();

    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());

    stubTransferPersistence();
    stubOutboxPersistence();
    stubTransaction();

    doNothing().when(accountClient).debit(SOURCE_ACCOUNT_ID, AMOUNT);

    doThrow(
            new AccountServiceException(
                "Destination credit failed",
                HttpStatus.CONFLICT,
                new RuntimeException("Account service rejected destination credit")))
        .when(accountClient)
        .credit(DESTINATION_ACCOUNT_ID, AMOUNT);

    doThrow(
            new AccountServiceException(
                "Compensation failed",
                HttpStatus.CONFLICT,
                new RuntimeException("Account service rejected compensation")))
        .when(accountClient)
        .credit(SOURCE_ACCOUNT_ID, AMOUNT);

    assertThrows(AccountServiceException.class, () -> transferService.createTransfer(request));

    verify(accountClient).debit(SOURCE_ACCOUNT_ID, AMOUNT);
    verify(accountClient).credit(DESTINATION_ACCOUNT_ID, AMOUNT);
    verify(accountClient).credit(SOURCE_ACCOUNT_ID, AMOUNT);

    verify(transferRepository, times(2)).save(any(Transfer.class));

    OutboxEvent outboxEvent = captureOutboxEvent();

    assertEquals(TRANSFER_ID, outboxEvent.getAggregateId());
    assertEquals("TRANSFER_FAILED", outboxEvent.getEventType());
    assertEquals(OutboxEventStatus.PENDING, outboxEvent.getStatus());

    AuditEventMessage message =
        objectMapper.readValue(outboxEvent.getPayload(), AuditEventMessage.class);

    assertEquals("FAILURE", message.outcome());
    assertEquals(
        "Transfer failed and the source account compensation also failed", message.description());
    assertEquals("FAILED", message.metadata().get("status"));
  }

  @Test
  void shouldReturnTransferById() {
    Transfer transfer = createTransfer();

    when(transferRepository.findById(TRANSFER_ID)).thenReturn(Optional.of(transfer));

    Transfer result = transferService.getById(TRANSFER_ID);

    assertSame(transfer, result);
  }

  @Test
  void shouldThrowWhenTransferIdDoesNotExist() {
    when(transferRepository.findById(TRANSFER_ID)).thenReturn(Optional.empty());

    assertThrows(TransferNotFoundException.class, () -> transferService.getById(TRANSFER_ID));
  }

  @Test
  void shouldReturnTransferByIdempotencyKey() {
    Transfer transfer = createTransfer();

    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
        .thenReturn(Optional.of(transfer));

    Transfer result = transferService.getByIdempotencyKey(IDEMPOTENCY_KEY);

    assertSame(transfer, result);
  }

  @Test
  void shouldThrowWhenIdempotencyKeyDoesNotExist() {
    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());

    assertThrows(
        TransferNotFoundException.class,
        () -> transferService.getByIdempotencyKey(IDEMPOTENCY_KEY));
  }

  @Test
  void shouldRejectSameIdempotencyKeyWithDifferentTransferDataWithoutCreatingOutboxEvent() {
    CreateTransferRequest request =
        new CreateTransferRequest(
            SOURCE_ACCOUNT_ID,
            DESTINATION_ACCOUNT_ID,
            new BigDecimal("500.0000"),
            "EUR",
            IDEMPOTENCY_KEY);

    Transfer existingTransfer =
        Transfer.create(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, AMOUNT, "EUR", IDEMPOTENCY_KEY);

    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
        .thenReturn(Optional.of(existingTransfer));

    assertThrows(IdempotencyConflictException.class, () -> transferService.createTransfer(request));

    verify(transferRepository, never()).save(any(Transfer.class));
    verify(outboxEventRepository, never()).save(any(OutboxEvent.class));

    verify(accountClient, never()).debit(any(), any());
    verify(accountClient, never()).credit(any(), any());
  }

  private void stubTransferPersistence() {
    when(transferRepository.save(any(Transfer.class)))
        .thenAnswer(
            invocation -> {
              Transfer transfer = invocation.getArgument(0);

              if (transfer.getId() == null) {
                ReflectionTestUtils.setField(transfer, "id", TRANSFER_ID);
              }

              return transfer;
            });
  }

  private void stubOutboxPersistence() {
    when(outboxEventRepository.save(any(OutboxEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  private void stubTransaction() {
    when(transactionManager.getTransaction(any(TransactionDefinition.class)))
        .thenReturn(new SimpleTransactionStatus());
  }

  private OutboxEvent captureOutboxEvent() {
    ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

    verify(outboxEventRepository).save(captor.capture());

    return captor.getValue();
  }

  private CreateTransferRequest createRequest() {
    return new CreateTransferRequest(
        SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, AMOUNT, "EUR", IDEMPOTENCY_KEY);
  }

  private Transfer createTransfer() {
    Transfer transfer =
        Transfer.create(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, AMOUNT, "EUR", IDEMPOTENCY_KEY);

    ReflectionTestUtils.setField(transfer, "id", TRANSFER_ID);

    return transfer;
  }
}
