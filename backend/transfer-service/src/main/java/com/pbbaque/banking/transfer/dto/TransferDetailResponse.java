package com.pbbaque.banking.transfer.dto;

import com.pbbaque.banking.transfer.domain.Transfer;
import com.pbbaque.banking.transfer.domain.TransferStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** API response containing the information required to display a transfer detail. */
public record TransferDetailResponse(
    UUID id,
    UUID sourceAccountId,
    String sourceIban,
    UUID destinationAccountId,
    String destinationIban,
    BigDecimal amount,
    String currency,
    TransferStatus status,
    String idempotencyKey,
    String failureReason,
    BigDecimal balanceAfter,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    OffsetDateTime completedAt) {

  /**
   * Creates a detailed response from a transfer and its related account information.
   *
   * @param transfer transfer
   * @param sourceIban source account IBAN
   * @param destinationIban destination account IBAN
   * @param balanceAfter resulting balance from the authenticated user's perspective
   * @return detailed transfer response
   */
  public static TransferDetailResponse from(
      Transfer transfer, String sourceIban, String destinationIban, BigDecimal balanceAfter) {

    return new TransferDetailResponse(
        transfer.getId(),
        transfer.getSourceAccountId(),
        sourceIban,
        transfer.getDestinationAccountId(),
        destinationIban,
        transfer.getAmount(),
        transfer.getCurrency(),
        transfer.getStatus(),
        transfer.getIdempotencyKey(),
        transfer.getFailureReason(),
        balanceAfter,
        transfer.getCreatedAt(),
        transfer.getUpdatedAt(),
        transfer.getCompletedAt());
  }
}
