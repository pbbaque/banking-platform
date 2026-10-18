package com.pbbaque.banking.transfer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pbbaque.banking.transfer.client.AccountClient;
import com.pbbaque.banking.transfer.domain.Transfer;
import com.pbbaque.banking.transfer.domain.TransferStatus;
import com.pbbaque.banking.transfer.dto.CreateTransferRequest;
import com.pbbaque.banking.transfer.exception.AccountServiceException;
import com.pbbaque.banking.transfer.exception.IdempotencyConflictException;
import com.pbbaque.banking.transfer.exception.TransferNotFoundException;
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

  @Mock private AccountClient accountClient;

  private TransferService transferService;

  @BeforeEach
  void setUp() {
    transferService = new TransferService(transferRepository, accountClient);
  }

  @Test
  void shouldCreateCompletedTransfer() {
    CreateTransferRequest request = createRequest();

    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());

    when(transferRepository.save(org.mockito.ArgumentMatchers.any(Transfer.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    doNothing().when(accountClient).debit(SOURCE_ACCOUNT_ID, AMOUNT);
    doNothing().when(accountClient).credit(DESTINATION_ACCOUNT_ID, AMOUNT);

    Transfer result = transferService.createTransfer(request);

    assertEquals(TransferStatus.COMPLETED, result.getStatus());

    verify(accountClient).debit(SOURCE_ACCOUNT_ID, AMOUNT);
    verify(accountClient).credit(DESTINATION_ACCOUNT_ID, AMOUNT);

    verify(transferRepository, org.mockito.Mockito.times(2))
        .save(org.mockito.ArgumentMatchers.any(Transfer.class));
  }

  @Test
  void shouldReturnExistingTransferForSameIdempotencyKey() {
    CreateTransferRequest request = createRequest();

    Transfer existingTransfer =
        Transfer.create(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, AMOUNT, "EUR", IDEMPOTENCY_KEY);

    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
        .thenReturn(Optional.of(existingTransfer));

    Transfer result = transferService.createTransfer(request);

    assertSame(existingTransfer, result);

    verify(transferRepository, never()).save(org.mockito.ArgumentMatchers.any(Transfer.class));

    verify(accountClient, never())
        .debit(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

    verify(accountClient, never())
        .credit(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldFailTransferWhenDebitFails() {
    CreateTransferRequest request = createRequest();

    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());

    when(transferRepository.save(org.mockito.ArgumentMatchers.any(Transfer.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

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

    verify(transferRepository, org.mockito.Mockito.times(2))
        .save(org.mockito.ArgumentMatchers.any(Transfer.class));
  }

  @Test
  void shouldCompensateSourceAccountWhenDestinationCreditFails() {
    CreateTransferRequest request = createRequest();

    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());

    when(transferRepository.save(org.mockito.ArgumentMatchers.any(Transfer.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

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

    verify(transferRepository, org.mockito.Mockito.times(2))
        .save(org.mockito.ArgumentMatchers.any(Transfer.class));
  }

  @Test
  void shouldThrowWhenDestinationCreditAndCompensationFail() {
    CreateTransferRequest request = createRequest();

    when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());

    when(transferRepository.save(org.mockito.ArgumentMatchers.any(Transfer.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

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

    verify(transferRepository, org.mockito.Mockito.times(2))
        .save(org.mockito.ArgumentMatchers.any(Transfer.class));
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
  void shouldRejectSameIdempotencyKeyWithDifferentTransferData() {
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

    verify(transferRepository, never()).save(org.mockito.ArgumentMatchers.any(Transfer.class));

    verify(accountClient, never())
        .debit(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

    verify(accountClient, never())
        .credit(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  private CreateTransferRequest createRequest() {
    return new CreateTransferRequest(
        SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, AMOUNT, "EUR", IDEMPOTENCY_KEY);
  }

  private Transfer createTransfer() {
    return Transfer.create(
        SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, AMOUNT, "EUR", IDEMPOTENCY_KEY);
  }
}
