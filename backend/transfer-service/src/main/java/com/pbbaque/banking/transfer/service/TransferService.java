package com.pbbaque.banking.transfer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbbaque.banking.transfer.client.AccountClient;
import com.pbbaque.banking.transfer.domain.OutboxEvent;
import com.pbbaque.banking.transfer.domain.Transfer;
import com.pbbaque.banking.transfer.domain.TransferProcessingStage;
import com.pbbaque.banking.transfer.domain.TransferStatus;
import com.pbbaque.banking.transfer.dto.AccountOperationResponse;
import com.pbbaque.banking.transfer.dto.AccountResponse;
import com.pbbaque.banking.transfer.dto.AuditEventMessage;
import com.pbbaque.banking.transfer.dto.CreateTransferRequest;
import com.pbbaque.banking.transfer.dto.TransferDetailResponse;
import com.pbbaque.banking.transfer.exception.AccountServiceException;
import com.pbbaque.banking.transfer.exception.IdempotencyConflictException;
import com.pbbaque.banking.transfer.exception.TransferNotFoundException;
import com.pbbaque.banking.transfer.repository.OutboxEventRepository;
import com.pbbaque.banking.transfer.repository.TransferRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Application service responsible for coordinating banking transfers. */
@Service
public class TransferService {

  private static final String AGGREGATE_TYPE = "TRANSFER";
  private static final String COMPLETED_EVENT_TYPE = "TRANSFER_COMPLETED";
  private static final String FAILED_EVENT_TYPE = "TRANSFER_FAILED";

  private final TransferRepository transferRepository;
  private final OutboxEventRepository outboxEventRepository;
  private final AccountClient accountClient;
  private final AccountOperationIdFactory accountOperationIdFactory;
  private final ObjectMapper objectMapper;
  private final TransactionTemplate transactionTemplate;

  /**
   * Creates the transfer service.
   *
   * @param transferRepository transfer repository
   * @param outboxEventRepository outbox event repository
   * @param accountClient client used to communicate with the account service
   * @param accountOperationIdFactory factory used to generate stable account operation identifiers
   * @param objectMapper JSON serializer
   * @param transactionManager transaction manager used for atomic transfer and outbox persistence
   */
  public TransferService(
      TransferRepository transferRepository,
      OutboxEventRepository outboxEventRepository,
      AccountClient accountClient,
      AccountOperationIdFactory accountOperationIdFactory,
      ObjectMapper objectMapper,
      PlatformTransactionManager transactionManager) {

    this.transferRepository = transferRepository;
    this.outboxEventRepository = outboxEventRepository;
    this.accountClient = accountClient;
    this.accountOperationIdFactory = accountOperationIdFactory;
    this.objectMapper = objectMapper.copy();
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  /**
   * Creates and processes a banking transfer.
   *
   * @param request transfer request
   * @param accessToken authenticated user's JWT
   * @param actorId authenticated user's identity identifier
   * @return processed transfer
   */
  public Transfer createTransfer(CreateTransferRequest request, String accessToken, UUID actorId) {

    requireAccountOwnership(request.sourceAccountId(), accessToken);

    AccountResponse destinationAccount =
        accountClient.getAccountByIban(request.destinationIban().trim());

    UUID destinationAccountId = destinationAccount.id();

    Transfer existingTransfer =
        transferRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);

    if (existingTransfer != null) {
      validateIdempotentRequest(existingTransfer, request, destinationAccountId);

      if (existingTransfer.isTerminal()) {
        return existingTransfer;
      }

      return resumeTransfer(existingTransfer, actorId);
    }

    Transfer transfer =
        Transfer.create(
            request.sourceAccountId(),
            destinationAccountId,
            request.amount(),
            request.currency(),
            request.idempotencyKey());

    transfer = transferRepository.save(transfer);

    return resumeTransfer(transfer, actorId);
  }

  /**
   * Returns a transfer by its identifier.
   *
   * @param transferId transfer identifier
   * @param accessToken authenticated user's JWT
   * @return accessible transfer
   */
  public Transfer getById(UUID transferId, String accessToken) {

    Transfer transfer =
        transferRepository
            .findById(transferId)
            .orElseThrow(() -> new TransferNotFoundException(transferId));

    requireTransferOwnership(transfer, accessToken);

    return transfer;
  }

  /**
   * Returns the complete information required to display a transfer detail.
   *
   * <p>The authenticated user must own either the source or destination account. Account
   * information is retrieved through the internal account-service API only after access to the
   * transfer has been validated.
   *
   * @param transferId transfer identifier
   * @param accessToken authenticated user's JWT
   * @return detailed transfer information
   */
  public TransferDetailResponse getDetailById(UUID transferId, String accessToken) {

    Transfer transfer = getById(transferId, accessToken);

    AccountResponse sourceAccount = accountClient.getAccount(transfer.getSourceAccountId());

    AccountResponse destinationAccount =
        accountClient.getAccount(transfer.getDestinationAccountId());

    BigDecimal balanceAfter = getBalanceAfterForUser(transfer, accessToken);

    return TransferDetailResponse.from(
        transfer, sourceAccount.iban(), destinationAccount.iban(), balanceAfter);
  }

  /**
   * Returns a transfer by its idempotency key.
   *
   * @param idempotencyKey transfer idempotency key
   * @param accessToken authenticated user's JWT
   * @return matching transfer
   */
  public Transfer getByIdempotencyKey(String idempotencyKey, String accessToken) {

    Transfer transfer =
        transferRepository
            .findByIdempotencyKey(idempotencyKey)
            .orElseThrow(() -> new TransferNotFoundException(idempotencyKey));

    requireAccountOwnership(transfer.getSourceAccountId(), accessToken);

    return transfer;
  }

  /**
   * Returns transfers involving an account owned by the authenticated user.
   *
   * @param accountId account identifier
   * @param accessToken authenticated user's JWT
   * @return transfers ordered newest first
   */
  public List<Transfer> getByAccountId(UUID accountId, String accessToken) {

    requireAccountOwnership(accountId, accessToken);

    return transferRepository.findAllBySourceAccountIdOrDestinationAccountIdOrderByCreatedAtDesc(
        accountId, accountId);
  }

  /**
   * Resolves the historical balance for a transfer from one account's perspective.
   *
   * <p>Outgoing completed transfers use their debit operation. Failed outgoing transfers prefer the
   * compensation operation when one exists. Incoming transfers use their credit operation.
   *
   * @param transfer transfer
   * @param accountId account from whose perspective the transfer is shown
   * @return resulting balance, or {@code null} when no balance operation exists
   */
  public BigDecimal getBalanceAfter(Transfer transfer, UUID accountId) {

    UUID transferId = requireTransferId(transfer);

    if (accountId.equals(transfer.getSourceAccountId())) {

      if (transfer.getStatus() == TransferStatus.FAILED) {

        AccountOperationResponse compensation =
            accountClient
                .findOperation(accountOperationIdFactory.compensation(transferId))
                .orElse(null);

        if (compensation != null) {
          return compensation.resultingBalance();
        }
      }

      return accountClient
          .findOperation(accountOperationIdFactory.debit(transferId))
          .map(AccountOperationResponse::resultingBalance)
          .orElse(null);
    }

    if (accountId.equals(transfer.getDestinationAccountId())) {

      return accountClient
          .findOperation(accountOperationIdFactory.credit(transferId))
          .map(AccountOperationResponse::resultingBalance)
          .orElse(null);
    }

    throw new IllegalArgumentException("Account does not belong to transfer");
  }

  /**
   * Resolves the historical balance from the authenticated user's perspective.
   *
   * @param transfer transfer
   * @param accessToken authenticated user's JWT
   * @return resulting balance, or {@code null} when no balance operation exists
   */
  public BigDecimal getBalanceAfterForUser(Transfer transfer, String accessToken) {

    try {
      requireAccountOwnership(transfer.getSourceAccountId(), accessToken);

      return getBalanceAfter(transfer, transfer.getSourceAccountId());

    } catch (AccountServiceException sourceException) {

      if (isCommunicationFailure(sourceException)) {
        throw sourceException;
      }

      requireAccountOwnership(transfer.getDestinationAccountId(), accessToken);

      return getBalanceAfter(transfer, transfer.getDestinationAccountId());
    }
  }

  private Transfer resumeTransfer(Transfer transfer, UUID actorId) {

    if (!transfer.isResumable()) {
      if (transfer.getProcessingStage() == TransferProcessingStage.RECOVERY_REQUIRED) {

        throw new IllegalStateException(
            "Transfer requires manual recovery because its processing stage is unknown");
      }

      return transfer;
    }

    return switch (transfer.getProcessingStage()) {
      case DEBIT_PENDING -> processDebit(transfer, actorId);

      case CREDIT_PENDING -> processCredit(transfer, actorId);

      case COMPENSATION_PENDING -> processCompensation(transfer, null, actorId);

      case TERMINAL -> transfer;

      case RECOVERY_REQUIRED ->
          throw new IllegalStateException(
              "Transfer requires manual recovery because its processing stage is unknown");
    };
  }

  private Transfer processDebit(Transfer transfer, UUID actorId) {

    UUID transferId = requireTransferId(transfer);

    UUID debitOperationId = accountOperationIdFactory.debit(transferId);

    try {
      accountClient.debit(transfer.getSourceAccountId(), transfer.getAmount(), debitOperationId);

    } catch (AccountServiceException exception) {

      if (isCommunicationFailure(exception)) {
        throw exception;
      }

      transfer.fail("Unable to debit source account");

      persistTransferWithAudit(
          transfer,
          FAILED_EVENT_TYPE,
          "FAILURE",
          "Transfer failed because the source account could not be debited",
          actorId);

      throw exception;
    }

    transfer.markDebitCompleted();

    transfer = transferRepository.save(transfer);

    return processCredit(transfer, actorId);
  }

  private Transfer processCredit(Transfer transfer, UUID actorId) {

    UUID transferId = requireTransferId(transfer);

    UUID creditOperationId = accountOperationIdFactory.credit(transferId);

    try {
      accountClient.credit(
          transfer.getDestinationAccountId(), transfer.getAmount(), creditOperationId);

    } catch (AccountServiceException creditException) {

      if (isCommunicationFailure(creditException)) {
        throw creditException;
      }

      transfer.markCompensationPending();

      transfer = transferRepository.save(transfer);

      return processCompensation(transfer, creditException, actorId);
    }

    transfer.complete();

    return persistTransferWithAudit(
        transfer, COMPLETED_EVENT_TYPE, "SUCCESS", "Transfer completed successfully", actorId);
  }

  private Transfer processCompensation(
      Transfer transfer, AccountServiceException originalException, UUID actorId) {

    UUID transferId = requireTransferId(transfer);

    UUID compensationOperationId = accountOperationIdFactory.compensation(transferId);

    try {
      accountClient.credit(
          transfer.getSourceAccountId(), transfer.getAmount(), compensationOperationId);

      transfer.fail("Destination account could not be credited; source debit was compensated");

      return persistTransferWithAudit(
          transfer,
          FAILED_EVENT_TYPE,
          "FAILURE",
          "Transfer failed because the destination account could not be credited; "
              + "the source debit was compensated",
          actorId);

    } catch (AccountServiceException compensationException) {

      if (isCommunicationFailure(compensationException)) {

        if (originalException != null) {
          compensationException.addSuppressed(originalException);
        }

        throw compensationException;
      }

      transfer.fail("Destination credit and source compensation both failed");

      persistTransferWithAudit(
          transfer,
          FAILED_EVENT_TYPE,
          "FAILURE",
          "Transfer failed and the source account compensation also failed",
          actorId);

      if (originalException != null) {
        compensationException.addSuppressed(originalException);
      }

      throw compensationException;
    }
  }

  private boolean isCommunicationFailure(AccountServiceException exception) {

    return exception.getStatusCode() == null;
  }

  private void requireAccountOwnership(UUID accountId, String accessToken) {

    accountClient.getOwnedAccount(accountId, accessToken);
  }

  private void requireTransferOwnership(Transfer transfer, String accessToken) {

    try {
      requireAccountOwnership(transfer.getSourceAccountId(), accessToken);

      return;

    } catch (AccountServiceException sourceException) {

      if (isCommunicationFailure(sourceException)) {
        throw sourceException;
      }

      try {
        requireAccountOwnership(transfer.getDestinationAccountId(), accessToken);

      } catch (AccountServiceException destinationException) {

        if (isCommunicationFailure(destinationException)) {
          throw destinationException;
        }

        destinationException.addSuppressed(sourceException);

        throw destinationException;
      }
    }
  }

  private Transfer persistTransferWithAudit(
      Transfer transfer, String eventType, String outcome, String description, UUID actorId) {

    UUID transferId = requireTransferId(transfer);

    UUID eventId = UUID.randomUUID();

    OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);

    AuditEventMessage event =
        new AuditEventMessage(
            eventId,
            eventType,
            AGGREGATE_TYPE,
            transferId.toString(),
            actorId,
            outcome,
            description,
            createMetadata(transfer),
            occurredAt);

    String payload = serializeEvent(event);

    OutboxEvent outboxEvent =
        OutboxEvent.create(eventId, transferId, eventType, payload, occurredAt);

    Transfer persistedTransfer =
        transactionTemplate.execute(
            status -> {
              Transfer savedTransfer = transferRepository.save(transfer);

              outboxEventRepository.save(outboxEvent);

              return savedTransfer;
            });

    if (persistedTransfer == null) {
      throw new IllegalStateException("Transfer and outbox transaction returned no result");
    }

    return persistedTransfer;
  }

  private Map<String, Object> createMetadata(Transfer transfer) {

    Map<String, Object> metadata = new LinkedHashMap<>();

    metadata.put("sourceAccountId", transfer.getSourceAccountId());

    metadata.put("destinationAccountId", transfer.getDestinationAccountId());

    metadata.put("amount", transfer.getAmount());

    metadata.put("currency", transfer.getCurrency());

    metadata.put("idempotencyKey", transfer.getIdempotencyKey());

    metadata.put("status", transfer.getStatus());

    metadata.put("processingStage", transfer.getProcessingStage());

    return metadata;
  }

  private String serializeEvent(AuditEventMessage event) {

    try {
      return objectMapper.writeValueAsString(event);

    } catch (JsonProcessingException exception) {

      throw new IllegalStateException("Unable to serialize transfer audit event", exception);
    }
  }

  private UUID requireTransferId(Transfer transfer) {

    UUID transferId = transfer.getId();

    if (transferId == null) {
      throw new IllegalStateException("Persisted transfer has no identifier");
    }

    return transferId;
  }

  private void validateIdempotentRequest(
      Transfer existingTransfer, CreateTransferRequest request, UUID destinationAccountId) {

    boolean sameRequest =
        existingTransfer.getSourceAccountId().equals(request.sourceAccountId())
            && existingTransfer.getDestinationAccountId().equals(destinationAccountId)
            && existingTransfer.getAmount().compareTo(request.amount()) == 0
            && existingTransfer.getCurrency().equalsIgnoreCase(request.currency().trim());

    if (!sameRequest) {
      throw new IdempotencyConflictException(request.idempotencyKey());
    }
  }
}
