package com.pbbaque.banking.transfer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbbaque.banking.transfer.client.AccountClient;
import com.pbbaque.banking.transfer.domain.OutboxEvent;
import com.pbbaque.banking.transfer.domain.Transfer;
import com.pbbaque.banking.transfer.domain.TransferProcessingStage;
import com.pbbaque.banking.transfer.dto.AuditEventMessage;
import com.pbbaque.banking.transfer.dto.CreateTransferRequest;
import com.pbbaque.banking.transfer.exception.AccountServiceException;
import com.pbbaque.banking.transfer.exception.IdempotencyConflictException;
import com.pbbaque.banking.transfer.exception.TransferNotFoundException;
import com.pbbaque.banking.transfer.repository.OutboxEventRepository;
import com.pbbaque.banking.transfer.repository.TransferRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
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
   * <p>The authenticated user must own the source account. Terminal transfers are returned directly
   * when the same idempotency key is replayed. Pending transfers are resumed from their persisted
   * processing stage.
   *
   * @param request transfer request
   * @param accessToken authenticated user's JWT
   * @return processed transfer
   */
  public Transfer createTransfer(CreateTransferRequest request, String accessToken) {
    requireSourceAccountOwnership(request.sourceAccountId(), accessToken);

    Transfer existingTransfer =
        transferRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);

    if (existingTransfer != null) {
      validateIdempotentRequest(existingTransfer, request);

      if (existingTransfer.isTerminal()) {
        return existingTransfer;
      }

      return resumeTransfer(existingTransfer);
    }

    Transfer transfer =
        Transfer.create(
            request.sourceAccountId(),
            request.destinationAccountId(),
            request.amount(),
            request.currency(),
            request.idempotencyKey());

    transfer = transferRepository.save(transfer);

    return resumeTransfer(transfer);
  }

  /**
   * Returns a transfer by its identifier.
   *
   * @param transferId transfer identifier
   * @param accessToken authenticated user's JWT
   * @return transfer when found and owned by the authenticated user
   */
  public Transfer getById(UUID transferId, String accessToken) {
    Transfer transfer =
        transferRepository
            .findById(transferId)
            .orElseThrow(() -> new TransferNotFoundException(transferId));

    requireSourceAccountOwnership(transfer.getSourceAccountId(), accessToken);

    return transfer;
  }

  /**
   * Returns a transfer by its idempotency key.
   *
   * @param idempotencyKey idempotency key
   * @param accessToken authenticated user's JWT
   * @return transfer when found and owned by the authenticated user
   */
  public Transfer getByIdempotencyKey(String idempotencyKey, String accessToken) {
    Transfer transfer =
        transferRepository
            .findByIdempotencyKey(idempotencyKey)
            .orElseThrow(() -> new TransferNotFoundException(idempotencyKey));

    requireSourceAccountOwnership(transfer.getSourceAccountId(), accessToken);

    return transfer;
  }

  private Transfer resumeTransfer(Transfer transfer) {
    if (!transfer.isResumable()) {
      if (transfer.getProcessingStage() == TransferProcessingStage.RECOVERY_REQUIRED) {
        throw new IllegalStateException(
            "Transfer requires manual recovery because its processing stage is unknown");
      }

      return transfer;
    }

    return switch (transfer.getProcessingStage()) {
      case DEBIT_PENDING -> processDebit(transfer);
      case CREDIT_PENDING -> processCredit(transfer);
      case COMPENSATION_PENDING -> processCompensation(transfer, null);
      case TERMINAL -> transfer;
      case RECOVERY_REQUIRED ->
          throw new IllegalStateException(
              "Transfer requires manual recovery because its processing stage is unknown");
    };
  }

  private Transfer processDebit(Transfer transfer) {
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
          "Transfer failed because the source account could not be debited");

      throw exception;
    }

    transfer.markDebitCompleted();
    transfer = transferRepository.save(transfer);

    return processCredit(transfer);
  }

  private Transfer processCredit(Transfer transfer) {
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

      return processCompensation(transfer, creditException);
    }

    transfer.complete();

    return persistTransferWithAudit(
        transfer, COMPLETED_EVENT_TYPE, "SUCCESS", "Transfer completed successfully");
  }

  private Transfer processCompensation(
      Transfer transfer, AccountServiceException originalException) {

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
              + "the source debit was compensated");

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
          "Transfer failed and the source account compensation also failed");

      if (originalException != null) {
        compensationException.addSuppressed(originalException);
      }

      throw compensationException;
    }
  }

  private boolean isCommunicationFailure(AccountServiceException exception) {
    return exception.getStatusCode() == null;
  }

  private void requireSourceAccountOwnership(UUID sourceAccountId, String accessToken) {

    accountClient.getOwnedAccount(sourceAccountId, accessToken);
  }

  private Transfer persistTransferWithAudit(
      Transfer transfer, String eventType, String outcome, String description) {

    UUID transferId = requireTransferId(transfer);
    UUID eventId = UUID.randomUUID();
    OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);

    AuditEventMessage event =
        new AuditEventMessage(
            eventId,
            eventType,
            AGGREGATE_TYPE,
            transferId.toString(),
            null,
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

  private void validateIdempotentRequest(Transfer existingTransfer, CreateTransferRequest request) {

    boolean sameRequest =
        existingTransfer.getSourceAccountId().equals(request.sourceAccountId())
            && existingTransfer.getDestinationAccountId().equals(request.destinationAccountId())
            && existingTransfer.getAmount().compareTo(request.amount()) == 0
            && existingTransfer.getCurrency().equalsIgnoreCase(request.currency().trim());

    if (!sameRequest) {
      throw new IdempotencyConflictException(request.idempotencyKey());
    }
  }
}
