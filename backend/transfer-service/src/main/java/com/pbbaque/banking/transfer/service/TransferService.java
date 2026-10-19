package com.pbbaque.banking.transfer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbbaque.banking.transfer.client.AccountClient;
import com.pbbaque.banking.transfer.domain.OutboxEvent;
import com.pbbaque.banking.transfer.domain.Transfer;
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
   * <p>If the supplied idempotency key was already processed, the existing transfer is returned
   * without executing the debit or credit again.
   *
   * @param request transfer request
   * @return processed transfer
   */
  public Transfer createTransfer(CreateTransferRequest request) {
    Transfer existingTransfer =
        transferRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);

    if (existingTransfer != null) {
      validateIdempotentRequest(existingTransfer, request);
      return existingTransfer;
    }

    Transfer transfer =
        Transfer.create(
            request.sourceAccountId(),
            request.destinationAccountId(),
            request.amount(),
            request.currency(),
            request.idempotencyKey());

    transfer = transferRepository.save(transfer);

    UUID transferId = requireTransferId(transfer);
    UUID debitOperationId = accountOperationIdFactory.debit(transferId);

    try {
      accountClient.debit(transfer.getSourceAccountId(), transfer.getAmount(), debitOperationId);
    } catch (AccountServiceException exception) {
      transfer.fail("Unable to debit source account");

      persistTransferWithAudit(
          transfer,
          FAILED_EVENT_TYPE,
          "FAILURE",
          "Transfer failed because the source account could not be debited");

      throw exception;
    }

    UUID creditOperationId = accountOperationIdFactory.credit(transferId);

    try {
      accountClient.credit(
          transfer.getDestinationAccountId(), transfer.getAmount(), creditOperationId);
    } catch (AccountServiceException creditException) {
      return compensateSourceAccount(transfer, creditException);
    }

    transfer.complete();

    return persistTransferWithAudit(
        transfer, COMPLETED_EVENT_TYPE, "SUCCESS", "Transfer completed successfully");
  }

  /**
   * Returns a transfer by its identifier.
   *
   * @param transferId transfer identifier
   * @return transfer when found
   */
  public Transfer getById(UUID transferId) {
    return transferRepository
        .findById(transferId)
        .orElseThrow(() -> new TransferNotFoundException(transferId));
  }

  /**
   * Returns a transfer by its idempotency key.
   *
   * @param idempotencyKey idempotency key
   * @return transfer when found
   */
  public Transfer getByIdempotencyKey(String idempotencyKey) {
    return transferRepository
        .findByIdempotencyKey(idempotencyKey)
        .orElseThrow(() -> new TransferNotFoundException(idempotencyKey));
  }

  private Transfer compensateSourceAccount(
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
      transfer.fail("Destination credit and source compensation both failed");

      persistTransferWithAudit(
          transfer,
          FAILED_EVENT_TYPE,
          "FAILURE",
          "Transfer failed and the source account compensation also failed");

      compensationException.addSuppressed(originalException);
      throw compensationException;
    }
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
