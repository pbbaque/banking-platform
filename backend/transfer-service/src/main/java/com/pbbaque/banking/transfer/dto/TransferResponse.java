package com.pbbaque.banking.transfer.dto;

import com.pbbaque.banking.transfer.domain.Transfer;
import com.pbbaque.banking.transfer.domain.TransferStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API response representing a banking transfer.
 *
 * @param id transfer identifier
 * @param sourceAccountId source account identifier
 * @param destinationAccountId destination account identifier
 * @param amount transferred amount
 * @param currency transfer currency
 * @param status current transfer status
 * @param idempotencyKey idempotency key associated with the request
 * @param failureReason failure reason when the transfer did not complete
 * @param createdAt transfer creation time
 * @param updatedAt last modification time
 * @param completedAt completion time when successful
 */
public record TransferResponse(
    UUID id,
    UUID sourceAccountId,
    UUID destinationAccountId,
    BigDecimal amount,
    String currency,
    TransferStatus status,
    String idempotencyKey,
    String failureReason,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    OffsetDateTime completedAt) {

  /**
   * Creates an API response from a transfer entity.
   *
   * @param transfer transfer entity
   * @return transfer response
   */
  public static TransferResponse from(Transfer transfer) {
    return new TransferResponse(
        transfer.getId(),
        transfer.getSourceAccountId(),
        transfer.getDestinationAccountId(),
        transfer.getAmount(),
        transfer.getCurrency(),
        transfer.getStatus(),
        transfer.getIdempotencyKey(),
        transfer.getFailureReason(),
        transfer.getCreatedAt(),
        transfer.getUpdatedAt(),
        transfer.getCompletedAt());
  }
}
