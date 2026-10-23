package com.pbbaque.banking.transfer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pbbaque.banking.transfer.client.AccountClient;
import com.pbbaque.banking.transfer.domain.OutboxEvent;
import com.pbbaque.banking.transfer.domain.Transfer;
import com.pbbaque.banking.transfer.domain.TransferProcessingStage;
import com.pbbaque.banking.transfer.domain.TransferStatus;
import com.pbbaque.banking.transfer.dto.AccountResponse;
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

  private static final UUID ACTOR_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

  private static final String DESTINATION_IBAN = "ES6421000418450200051333";

  private static final BigDecimal AMOUNT = new BigDecimal("100.0000");

  private static final String IDEMPOTENCY_KEY = "transfer-key-1";

  private static final String ACCESS_TOKEN = "test-access-token";

  @Mock private TransferRepository transferRepository;

  @Mock private OutboxEventRepository outboxEventRepository;

  @Mock private AccountClient accountClient;

  @Mock private PlatformTransactionManager transactionManager;

  private AccountOperationIdFactory accountOperationIdFactory;

  private TransferService transferService;

  @BeforeEach
  void setUp() {
    accountOperationIdFactory = new AccountOperationIdFactory();

    ObjectMapper objectMapper = new ObjectMapper();

    objectMapper.registerModule(new JavaTimeModule());

    transferService =
        new TransferService(
            transferRepository,
            outboxEventRepository,
            accountClient,
            accountOperationIdFactory,
            objectMapper,
            transactionManager);
  }

  @Test
  void shouldCreateCompletedTransferAndOutboxEvent() {
    prepareDestinationAccountLookup();
    prepareNewTransferPersistence();
    prepareTransactionManager();

    UUID debitOperationId = accountOperationIdFactory.debit(TRANSFER_ID);

    UUID creditOperationId = accountOperationIdFactory.credit(TRANSFER_ID);

    Transfer result = transferService.createTransfer(createRequest(), ACCESS_TOKEN, ACTOR_ID);

    assertEquals(TransferStatus.COMPLETED, result.getStatus());

    assertEquals(TransferProcessingStage.TERMINAL, result.getProcessingStage());

    verify(accountClient).getOwnedAccount(SOURCE_ACCOUNT_ID, ACCESS_TOKEN);

    verify(accountClient).getAccountByIban(DESTINATION_IBAN);

    verify(accountClient).debit(SOURCE_ACCOUNT_ID, AMOUNT, debitOperationId);

    verify(accountClient).credit(DESTINATION_ACCOUNT_ID, AMOUNT, creditOperationId);

    verify(transferRepository, times(3)).save(any(Transfer.class));

    verify(outboxEventRepository).save(any(OutboxEvent.class));
  }

  @Test
  void shouldReturnExistingTerminalTransferWithoutProcessingAgain() {
    prepareDestinationAccountLookup();

    Transfer existingTransfer = createCompletedTransfer();

    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
        .thenReturn(Optional.of(existingTransfer));

    Transfer result = transferService.createTransfer(createRequest(), ACCESS_TOKEN, ACTOR_ID);

    assertSame(existingTransfer, result);

    verify(accountClient).getOwnedAccount(SOURCE_ACCOUNT_ID, ACCESS_TOKEN);

    verify(accountClient).getAccountByIban(DESTINATION_IBAN);

    verify(accountClient, never()).debit(any(), any(), any());

    verify(accountClient, never()).credit(any(), any(), any());

    verify(transferRepository, never()).save(any(Transfer.class));

    verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
  }

  @Test
  void shouldResumeTransferFromDebitPending() {
    prepareDestinationAccountLookup();
    prepareTransferSave();
    prepareTransactionManager();

    Transfer transfer = createTransferAtStage(TransferProcessingStage.DEBIT_PENDING);

    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
        .thenReturn(Optional.of(transfer));

    UUID debitOperationId = accountOperationIdFactory.debit(TRANSFER_ID);

    UUID creditOperationId = accountOperationIdFactory.credit(TRANSFER_ID);

    Transfer result = transferService.createTransfer(createRequest(), ACCESS_TOKEN, ACTOR_ID);

    assertEquals(TransferStatus.COMPLETED, result.getStatus());

    assertEquals(TransferProcessingStage.TERMINAL, result.getProcessingStage());

    verify(accountClient).debit(SOURCE_ACCOUNT_ID, AMOUNT, debitOperationId);

    verify(accountClient).credit(DESTINATION_ACCOUNT_ID, AMOUNT, creditOperationId);

    verify(transferRepository, times(2)).save(any(Transfer.class));

    verify(outboxEventRepository).save(any(OutboxEvent.class));
  }

  @Test
  void shouldResumeTransferFromCreditPendingWithoutRepeatingDebit() {
    prepareDestinationAccountLookup();
    prepareTransferSave();
    prepareTransactionManager();

    Transfer transfer = createTransferAtStage(TransferProcessingStage.CREDIT_PENDING);

    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
        .thenReturn(Optional.of(transfer));

    UUID creditOperationId = accountOperationIdFactory.credit(TRANSFER_ID);

    Transfer result = transferService.createTransfer(createRequest(), ACCESS_TOKEN, ACTOR_ID);

    assertEquals(TransferStatus.COMPLETED, result.getStatus());

    verify(accountClient, never()).debit(any(), any(), any());

    verify(accountClient).credit(DESTINATION_ACCOUNT_ID, AMOUNT, creditOperationId);

    verify(transferRepository).save(any(Transfer.class));

    verify(outboxEventRepository).save(any(OutboxEvent.class));
  }

  @Test
  void shouldResumeTransferFromCompensationPending() {
    prepareDestinationAccountLookup();
    prepareTransferSave();
    prepareTransactionManager();

    Transfer transfer = createTransferAtStage(TransferProcessingStage.COMPENSATION_PENDING);

    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
        .thenReturn(Optional.of(transfer));

    UUID compensationOperationId = accountOperationIdFactory.compensation(TRANSFER_ID);

    Transfer result = transferService.createTransfer(createRequest(), ACCESS_TOKEN, ACTOR_ID);

    assertEquals(TransferStatus.FAILED, result.getStatus());

    assertEquals(TransferProcessingStage.TERMINAL, result.getProcessingStage());

    assertEquals(
        "Destination account could not be credited; source debit was compensated",
        result.getFailureReason());

    verify(accountClient, never()).debit(any(), any(), any());

    verify(accountClient).credit(SOURCE_ACCOUNT_ID, AMOUNT, compensationOperationId);

    verify(transferRepository).save(any(Transfer.class));

    verify(outboxEventRepository).save(any(OutboxEvent.class));
  }

  @Test
  void shouldRejectAutomaticRecoveryWhenProcessingStageIsUnknown() {
    prepareDestinationAccountLookup();

    Transfer transfer = createTransferAtStage(TransferProcessingStage.RECOVERY_REQUIRED);

    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
        .thenReturn(Optional.of(transfer));

    assertThrows(
        IllegalStateException.class,
        () -> transferService.createTransfer(createRequest(), ACCESS_TOKEN, ACTOR_ID));

    verify(accountClient, never()).debit(any(), any(), any());

    verify(accountClient, never()).credit(any(), any(), any());

    verify(transferRepository, never()).save(any(Transfer.class));

    verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
  }

  @Test
  void shouldFailTransferAndCreateOutboxEventWhenDebitIsRejected() {
    prepareDestinationAccountLookup();
    prepareNewTransferPersistence();
    prepareTransactionManager();

    UUID debitOperationId = accountOperationIdFactory.debit(TRANSFER_ID);

    AccountServiceException exception =
        new AccountServiceException(
            "Debit failed",
            HttpStatus.CONFLICT,
            new RuntimeException("Account service rejected debit"));

    when(accountClient.debit(SOURCE_ACCOUNT_ID, AMOUNT, debitOperationId)).thenThrow(exception);

    AccountServiceException thrown =
        assertThrows(
            AccountServiceException.class,
            () -> transferService.createTransfer(createRequest(), ACCESS_TOKEN, ACTOR_ID));

    assertSame(exception, thrown);

    verify(accountClient, never()).credit(any(), any(), any());

    verify(transferRepository, times(2)).save(any(Transfer.class));

    verify(outboxEventRepository).save(any(OutboxEvent.class));
  }

  @Test
  void shouldKeepDebitPendingWhenDebitCommunicationFails() {
    prepareDestinationAccountLookup();
    prepareNewTransferPersistence();

    UUID debitOperationId = accountOperationIdFactory.debit(TRANSFER_ID);

    AccountServiceException exception =
        new AccountServiceException(
            "Unable to communicate with account service", new RuntimeException("Connection lost"));

    when(accountClient.debit(SOURCE_ACCOUNT_ID, AMOUNT, debitOperationId)).thenThrow(exception);

    AccountServiceException thrown =
        assertThrows(
            AccountServiceException.class,
            () -> transferService.createTransfer(createRequest(), ACCESS_TOKEN, ACTOR_ID));

    assertSame(exception, thrown);

    verify(transferRepository).save(any(Transfer.class));

    verify(outboxEventRepository, never()).save(any(OutboxEvent.class));

    verify(accountClient, never()).credit(any(), any(), any());
  }

  @Test
  void shouldKeepCreditPendingWhenCreditCommunicationFails() {
    prepareDestinationAccountLookup();
    prepareNewTransferPersistence();

    UUID creditOperationId = accountOperationIdFactory.credit(TRANSFER_ID);

    AccountServiceException exception =
        new AccountServiceException(
            "Unable to communicate with account service", new RuntimeException("Connection lost"));

    when(accountClient.credit(DESTINATION_ACCOUNT_ID, AMOUNT, creditOperationId))
        .thenThrow(exception);

    AccountServiceException thrown =
        assertThrows(
            AccountServiceException.class,
            () -> transferService.createTransfer(createRequest(), ACCESS_TOKEN, ACTOR_ID));

    assertSame(exception, thrown);

    verify(transferRepository, times(2)).save(any(Transfer.class));

    verify(outboxEventRepository, never()).save(any(OutboxEvent.class));

    verify(accountClient, never())
        .credit(SOURCE_ACCOUNT_ID, AMOUNT, accountOperationIdFactory.compensation(TRANSFER_ID));
  }

  @Test
  void shouldCompensateWhenDestinationCreditIsRejected() {
    prepareDestinationAccountLookup();
    prepareNewTransferPersistence();
    prepareTransactionManager();

    UUID creditOperationId = accountOperationIdFactory.credit(TRANSFER_ID);

    UUID compensationOperationId = accountOperationIdFactory.compensation(TRANSFER_ID);

    AccountServiceException creditException =
        new AccountServiceException(
            "Destination credit failed",
            HttpStatus.CONFLICT,
            new RuntimeException("Account service rejected destination credit"));

    when(accountClient.credit(DESTINATION_ACCOUNT_ID, AMOUNT, creditOperationId))
        .thenThrow(creditException);

    Transfer result = transferService.createTransfer(createRequest(), ACCESS_TOKEN, ACTOR_ID);

    assertEquals(TransferStatus.FAILED, result.getStatus());

    assertEquals(TransferProcessingStage.TERMINAL, result.getProcessingStage());

    assertEquals(
        "Destination account could not be credited; source debit was compensated",
        result.getFailureReason());

    verify(accountClient).credit(SOURCE_ACCOUNT_ID, AMOUNT, compensationOperationId);

    verify(transferRepository, times(4)).save(any(Transfer.class));

    verify(outboxEventRepository).save(any(OutboxEvent.class));
  }

  @Test
  void shouldFailTerminallyWhenCompensationIsRejected() {
    prepareDestinationAccountLookup();
    prepareNewTransferPersistence();
    prepareTransactionManager();

    UUID creditOperationId = accountOperationIdFactory.credit(TRANSFER_ID);

    UUID compensationOperationId = accountOperationIdFactory.compensation(TRANSFER_ID);

    AccountServiceException creditException =
        new AccountServiceException(
            "Destination credit failed",
            HttpStatus.CONFLICT,
            new RuntimeException("Account service rejected destination credit"));

    AccountServiceException compensationException =
        new AccountServiceException(
            "Compensation failed",
            HttpStatus.CONFLICT,
            new RuntimeException("Account service rejected compensation"));

    when(accountClient.credit(DESTINATION_ACCOUNT_ID, AMOUNT, creditOperationId))
        .thenThrow(creditException);

    when(accountClient.credit(SOURCE_ACCOUNT_ID, AMOUNT, compensationOperationId))
        .thenThrow(compensationException);

    AccountServiceException thrown =
        assertThrows(
            AccountServiceException.class,
            () -> transferService.createTransfer(createRequest(), ACCESS_TOKEN, ACTOR_ID));

    assertSame(compensationException, thrown);

    assertEquals(1, thrown.getSuppressed().length);

    assertSame(creditException, thrown.getSuppressed()[0]);

    verify(transferRepository, times(4)).save(any(Transfer.class));

    verify(outboxEventRepository).save(any(OutboxEvent.class));
  }

  @Test
  void shouldKeepCompensationPendingWhenCommunicationFails() {
    prepareDestinationAccountLookup();
    prepareNewTransferPersistence();

    UUID creditOperationId = accountOperationIdFactory.credit(TRANSFER_ID);

    UUID compensationOperationId = accountOperationIdFactory.compensation(TRANSFER_ID);

    AccountServiceException creditException =
        new AccountServiceException(
            "Destination credit failed",
            HttpStatus.CONFLICT,
            new RuntimeException("Account service rejected destination credit"));

    AccountServiceException compensationException =
        new AccountServiceException(
            "Unable to communicate with account service", new RuntimeException("Connection lost"));

    when(accountClient.credit(DESTINATION_ACCOUNT_ID, AMOUNT, creditOperationId))
        .thenThrow(creditException);

    when(accountClient.credit(SOURCE_ACCOUNT_ID, AMOUNT, compensationOperationId))
        .thenThrow(compensationException);

    AccountServiceException thrown =
        assertThrows(
            AccountServiceException.class,
            () -> transferService.createTransfer(createRequest(), ACCESS_TOKEN, ACTOR_ID));

    assertSame(compensationException, thrown);

    assertEquals(1, thrown.getSuppressed().length);

    assertSame(creditException, thrown.getSuppressed()[0]);

    verify(transferRepository, times(3)).save(any(Transfer.class));

    verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
  }

  @Test
  void shouldReturnTransferById() {
    Transfer transfer = createTransfer();

    when(transferRepository.findById(TRANSFER_ID)).thenReturn(Optional.of(transfer));

    Transfer result = transferService.getById(TRANSFER_ID, ACCESS_TOKEN);

    assertSame(transfer, result);

    verify(accountClient).getOwnedAccount(SOURCE_ACCOUNT_ID, ACCESS_TOKEN);
  }

  @Test
  void shouldThrowWhenTransferIdDoesNotExist() {
    when(transferRepository.findById(TRANSFER_ID)).thenReturn(Optional.empty());

    assertThrows(
        TransferNotFoundException.class, () -> transferService.getById(TRANSFER_ID, ACCESS_TOKEN));

    verify(accountClient, never()).getOwnedAccount(any(), any());
  }

  @Test
  void shouldReturnTransferByIdempotencyKey() {
    Transfer transfer = createTransfer();

    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
        .thenReturn(Optional.of(transfer));

    Transfer result = transferService.getByIdempotencyKey(IDEMPOTENCY_KEY, ACCESS_TOKEN);

    assertSame(transfer, result);

    verify(accountClient).getOwnedAccount(SOURCE_ACCOUNT_ID, ACCESS_TOKEN);
  }

  @Test
  void shouldThrowWhenIdempotencyKeyDoesNotExist() {
    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());

    assertThrows(
        TransferNotFoundException.class,
        () -> transferService.getByIdempotencyKey(IDEMPOTENCY_KEY, ACCESS_TOKEN));

    verify(accountClient, never()).getOwnedAccount(any(), any());
  }

  @Test
  void shouldRejectSameIdempotencyKeyWithDifferentTransferData() {
    prepareDestinationAccountLookup();

    CreateTransferRequest request =
        new CreateTransferRequest(
            SOURCE_ACCOUNT_ID,
            DESTINATION_IBAN,
            new BigDecimal("500.0000"),
            "EUR",
            IDEMPOTENCY_KEY);

    Transfer existingTransfer = createCompletedTransfer();

    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
        .thenReturn(Optional.of(existingTransfer));

    assertThrows(
        IdempotencyConflictException.class,
        () -> transferService.createTransfer(request, ACCESS_TOKEN, ACTOR_ID));

    verify(transferRepository, never()).save(any(Transfer.class));

    verify(accountClient, never()).debit(any(), any(), any());

    verify(accountClient, never()).credit(any(), any(), any());
  }

  @Test
  void shouldRejectTransferWhenSourceAccountIsNotOwned() {
    AccountServiceException exception =
        new AccountServiceException(
            "Account service rejected account ownership check",
            HttpStatus.FORBIDDEN,
            new RuntimeException("Forbidden"));

    when(accountClient.getOwnedAccount(SOURCE_ACCOUNT_ID, ACCESS_TOKEN)).thenThrow(exception);

    AccountServiceException thrown =
        assertThrows(
            AccountServiceException.class,
            () -> transferService.createTransfer(createRequest(), ACCESS_TOKEN, ACTOR_ID));

    assertSame(exception, thrown);

    verify(accountClient, never()).getAccountByIban(any());

    verify(transferRepository, never()).findByIdempotencyKey(any());

    verify(transferRepository, never()).save(any(Transfer.class));

    verify(accountClient, never()).debit(any(), any(), any());

    verify(accountClient, never()).credit(any(), any(), any());
  }

  private void prepareDestinationAccountLookup() {
    AccountResponse destinationAccount = mock(AccountResponse.class);

    when(destinationAccount.id()).thenReturn(DESTINATION_ACCOUNT_ID);

    when(accountClient.getAccountByIban(DESTINATION_IBAN)).thenReturn(destinationAccount);
  }

  private void prepareNewTransferPersistence() {
    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());

    prepareTransferSave();
  }

  private void prepareTransferSave() {
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

  private void prepareTransactionManager() {
    when(transactionManager.getTransaction(any(TransactionDefinition.class)))
        .thenReturn(new SimpleTransactionStatus());
  }

  private CreateTransferRequest createRequest() {
    return new CreateTransferRequest(
        SOURCE_ACCOUNT_ID, DESTINATION_IBAN, AMOUNT, "EUR", IDEMPOTENCY_KEY);
  }

  private Transfer createTransfer() {
    Transfer transfer =
        Transfer.create(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, AMOUNT, "EUR", IDEMPOTENCY_KEY);

    ReflectionTestUtils.setField(transfer, "id", TRANSFER_ID);

    return transfer;
  }

  private Transfer createCompletedTransfer() {
    Transfer transfer = createTransfer();

    transfer.complete();

    return transfer;
  }

  private Transfer createTransferAtStage(TransferProcessingStage stage) {

    Transfer transfer = createTransfer();

    switch (stage) {
      case DEBIT_PENDING -> {
        return transfer;
      }

      case CREDIT_PENDING -> transfer.markDebitCompleted();

      case COMPENSATION_PENDING -> {
        transfer.markDebitCompleted();
        transfer.markCompensationPending();
      }

      case RECOVERY_REQUIRED ->
          ReflectionTestUtils.setField(
              transfer, "processingStage", TransferProcessingStage.RECOVERY_REQUIRED);

      case TERMINAL -> transfer.complete();
    }

    return transfer;
  }
}
