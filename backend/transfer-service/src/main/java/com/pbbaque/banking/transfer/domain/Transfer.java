package com.pbbaque.banking.transfer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

/** Represents a banking transfer between two accounts. */
@Entity
@Table(name = "TRANSFERS")
public class Transfer {

  private static final String SUPPORTED_CURRENCY = "EUR";

  @Id
  @GeneratedValue
  @Column(name = "ID", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "SOURCE_ACCOUNT_ID", nullable = false, updatable = false)
  private UUID sourceAccountId;

  @Column(name = "DESTINATION_ACCOUNT_ID", nullable = false, updatable = false)
  private UUID destinationAccountId;

  @Column(name = "AMOUNT", nullable = false, precision = 19, scale = 4, updatable = false)
  private BigDecimal amount;

  @Column(name = "CURRENCY", nullable = false, length = 3, updatable = false)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS", nullable = false, length = 20)
  private TransferStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "PROCESSING_STAGE", nullable = false, length = 30)
  private TransferProcessingStage processingStage;

  @Column(name = "IDEMPOTENCY_KEY", nullable = false, unique = true, updatable = false)
  private String idempotencyKey;

  @Column(name = "FAILURE_REASON")
  private String failureReason;

  @Column(name = "CREATED_AT", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "UPDATED_AT", nullable = false)
  private OffsetDateTime updatedAt;

  @Column(name = "COMPLETED_AT")
  private OffsetDateTime completedAt;

  protected Transfer() {}

  private Transfer(
      UUID sourceAccountId,
      UUID destinationAccountId,
      BigDecimal amount,
      String currency,
      String idempotencyKey) {

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    this.sourceAccountId = sourceAccountId;
    this.destinationAccountId = destinationAccountId;
    this.amount = amount;
    this.currency = currency;
    this.idempotencyKey = idempotencyKey;
    this.status = TransferStatus.PENDING;
    this.processingStage = TransferProcessingStage.DEBIT_PENDING;
    this.createdAt = now;
    this.updatedAt = now;
  }

  /**
   * Creates a new pending transfer.
   *
   * @param sourceAccountId source account identifier
   * @param destinationAccountId destination account identifier
   * @param amount transfer amount
   * @param currency transfer currency
   * @param idempotencyKey idempotency key
   * @return newly created transfer
   */
  public static Transfer create(
      UUID sourceAccountId,
      UUID destinationAccountId,
      BigDecimal amount,
      String currency,
      String idempotencyKey) {

    validateAccountIds(sourceAccountId, destinationAccountId);
    validateAmount(amount);

    String normalizedCurrency = normalizeCurrency(currency);
    validateCurrency(normalizedCurrency);

    String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);

    return new Transfer(
        sourceAccountId,
        destinationAccountId,
        amount,
        normalizedCurrency,
        normalizedIdempotencyKey);
  }

  /** Marks the source debit as completed. */
  public void markDebitCompleted() {
    requirePending();

    if (processingStage != TransferProcessingStage.DEBIT_PENDING) {
      throw new IllegalStateException("Transfer is not waiting for source account debit");
    }

    processingStage = TransferProcessingStage.CREDIT_PENDING;
    touch();
  }

  /** Marks the transfer as waiting for source-account compensation. */
  public void markCompensationPending() {
    requirePending();

    if (processingStage != TransferProcessingStage.CREDIT_PENDING) {
      throw new IllegalStateException("Transfer is not waiting for destination account credit");
    }

    processingStage = TransferProcessingStage.COMPENSATION_PENDING;
    touch();
  }

  /**
   * Marks this transfer as completed.
   *
   * <p>The public domain operation remains compatible with the original transfer aggregate and can
   * complete any pending transfer. Normal application processing still reaches this method from
   * {@link TransferProcessingStage#CREDIT_PENDING}.
   */
  public void complete() {
    requirePending();

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    status = TransferStatus.COMPLETED;
    processingStage = TransferProcessingStage.TERMINAL;
    failureReason = null;
    completedAt = now;
    updatedAt = now;
  }

  /**
   * Marks this transfer as failed.
   *
   * @param reason failure reason
   */
  public void fail(String reason) {
    requirePending();

    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("Failure reason is required");
    }

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    status = TransferStatus.FAILED;
    processingStage = TransferProcessingStage.TERMINAL;
    failureReason = reason.trim();
    completedAt = null;
    updatedAt = now;
  }

  /**
   * Returns whether this transfer has reached a final state.
   *
   * @return {@code true} for completed or failed transfers
   */
  public boolean isTerminal() {
    return status == TransferStatus.COMPLETED || status == TransferStatus.FAILED;
  }

  /**
   * Returns whether automatic processing can safely resume.
   *
   * @return {@code true} when the transfer has a known pending stage
   */
  public boolean isResumable() {
    return status == TransferStatus.PENDING
        && processingStage != TransferProcessingStage.RECOVERY_REQUIRED
        && processingStage != TransferProcessingStage.TERMINAL;
  }

  @PrePersist
  void prePersist() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    if (createdAt == null) {
      createdAt = now;
    }

    if (updatedAt == null) {
      updatedAt = now;
    }

    if (status == null) {
      status = TransferStatus.PENDING;
    }

    if (processingStage == null) {
      processingStage = TransferProcessingStage.DEBIT_PENDING;
    }
  }

  private static void validateAccountIds(UUID sourceAccountId, UUID destinationAccountId) {

    if (sourceAccountId == null) {
      throw new IllegalArgumentException("Source account id is required");
    }

    if (destinationAccountId == null) {
      throw new IllegalArgumentException("Destination account id is required");
    }

    if (sourceAccountId.equals(destinationAccountId)) {
      throw new IllegalArgumentException("Source and destination accounts must be different");
    }
  }

  private static void validateAmount(BigDecimal amount) {
    if (amount == null) {
      throw new IllegalArgumentException("Transfer amount is required");
    }

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Transfer amount must be greater than zero");
    }

    if (amount.scale() > 4) {
      throw new IllegalArgumentException(
          "Transfer amount cannot have more than four decimal places");
    }
  }

  private static String normalizeCurrency(String currency) {
    if (currency == null || currency.isBlank()) {
      throw new IllegalArgumentException("Transfer currency is required");
    }

    return currency.trim().toUpperCase(Locale.ROOT);
  }

  private static void validateCurrency(String currency) {
    if (!SUPPORTED_CURRENCY.equals(currency)) {
      throw new IllegalArgumentException("Only EUR transfers are supported");
    }
  }

  private static String normalizeIdempotencyKey(String idempotencyKey) {

    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new IllegalArgumentException("Idempotency key is required");
    }

    return idempotencyKey.trim();
  }

  private void requirePending() {
    if (status != TransferStatus.PENDING) {
      throw new IllegalStateException("Only pending transfers can change state");
    }
  }

  private void touch() {
    updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public UUID getId() {
    return id;
  }

  public UUID getSourceAccountId() {
    return sourceAccountId;
  }

  public UUID getDestinationAccountId() {
    return destinationAccountId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public TransferStatus getStatus() {
    return status;
  }

  public TransferProcessingStage getProcessingStage() {
    return processingStage;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public OffsetDateTime getCompletedAt() {
    return completedAt;
  }
}
