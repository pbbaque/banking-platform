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
  private final ObjectMapper objectMapper;
  private final TransactionTemplate transactionTemplate;

  /**
   * Creates the transfer service.
   *
   * @param transferRepository transfer repository
   * @param outboxEventRepository outbox event repository
   * @param accountClient client used to communicate with the account service
   * @param objectMapper JSON serializer
   * @param transactionManager transaction manager used for atomic transfer and outbox persistence
   */
  public TransferService(
      TransferRepository transferRepository,
      OutboxEventRepository outboxEventRepository,
      AccountClient accountClient,
      ObjectMapper objectMapper,
      PlatformTransactionManager transactionManager) {
    this.transferRepository = transferRepository;
    this.outboxEventRepository = outboxEventRepository;
    this.accountClient = accountClient;
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

    try {
      accountClient.debit(transfer.getSourceAccountId(), transfer.getAmount());
    } catch (AccountServiceException exception) {
      transfer.fail("Unable to debit source account");

      persistTransferWithAudit(
          transfer,
          FAILED_EVENT_TYPE,
          "FAILURE",
          "Transfer failed because the source account could not be debited");

      throw exception;
    }

    try {
      accountClient.credit(transfer.getDestinationAccountId(), transfer.getAmount());
    } catch (AccountServiceException creditException) {
      compensateSourceAccount(transfer, creditException);
      throw creditException;
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

  private void compensateSourceAccount(
      Transfer transfer, AccountServiceException originalException) {

    try {
      accountClient.credit(transfer.getSourceAccountId(), transfer.getAmount());

      transfer.fail("Destination account could not be credited; source debit was compensated");

      persistTransferWithAudit(
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

    UUID eventId = UUID.randomUUID();
    OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);

    AuditEventMessage event =
        new AuditEventMessage(
            eventId,
            eventType,
            AGGREGATE_TYPE,
            transfer.getId().toString(),
            null,
            outcome,
            description,
            createMetadata(transfer),
            occurredAt);

    String payload = serializeEvent(event);

    OutboxEvent outboxEvent =
        OutboxEvent.create(eventId, transfer.getId(), eventType, payload, occurredAt);

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
