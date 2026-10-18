package com.pbbaque.banking.transfer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Represents a banking transfer between two accounts. */
@Entity
@Table(name = "TRANSFERS")
public class Transfer {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @JdbcTypeCode(SqlTypes.BINARY)
  @Column(name = "ID", nullable = false, updatable = false, columnDefinition = "RAW(16)")
  private UUID id;

  @JdbcTypeCode(SqlTypes.BINARY)
  @Column(
      name = "SOURCE_ACCOUNT_ID",
      nullable = false,
      updatable = false,
      columnDefinition = "RAW(16)")
  private UUID sourceAccountId;

  @JdbcTypeCode(SqlTypes.BINARY)
  @Column(
      name = "DESTINATION_ACCOUNT_ID",
      nullable = false,
      updatable = false,
      columnDefinition = "RAW(16)")
  private UUID destinationAccountId;

  @Column(name = "AMOUNT", nullable = false, precision = 19, scale = 4, updatable = false)
  private BigDecimal amount;

  @Column(name = "CURRENCY", nullable = false, length = 3, updatable = false)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS", nullable = false, length = 30)
  private TransferStatus status;

  @Column(
      name = "IDEMPOTENCY_KEY",
      nullable = false,
      unique = true,
      length = 100,
      updatable = false)
  private String idempotencyKey;

  @Column(name = "FAILURE_REASON", length = 500)
  private String failureReason;

  @Column(name = "CREATED_AT", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "UPDATED_AT", nullable = false)
  private OffsetDateTime updatedAt;

  @Column(name = "COMPLETED_AT")
  private OffsetDateTime completedAt;

  /** Required by JPA. */
  protected Transfer() {}

  private Transfer(
      UUID sourceAccountId,
      UUID destinationAccountId,
      BigDecimal amount,
      String currency,
      String idempotencyKey) {
    this.sourceAccountId = sourceAccountId;
    this.destinationAccountId = destinationAccountId;
    this.amount = amount;
    this.currency = currency;
    this.idempotencyKey = idempotencyKey;
    this.status = TransferStatus.PENDING;
  }

  /**
   * Creates a new pending banking transfer.
   *
   * @param sourceAccountId source account identifier
   * @param destinationAccountId destination account identifier
   * @param amount amount to transfer
   * @param currency transfer currency
   * @param idempotencyKey unique idempotency key for the transfer request
   * @return a new pending transfer
   */
  public static Transfer create(
      UUID sourceAccountId,
      UUID destinationAccountId,
      BigDecimal amount,
      String currency,
      String idempotencyKey) {

    if (sourceAccountId == null) {
      throw new IllegalArgumentException("Source account id must not be null");
    }

    if (destinationAccountId == null) {
      throw new IllegalArgumentException("Destination account id must not be null");
    }

    if (sourceAccountId.equals(destinationAccountId)) {
      throw new IllegalArgumentException("Source and destination accounts must be different");
    }

    if (amount == null || amount.signum() <= 0) {
      throw new IllegalArgumentException("Transfer amount must be greater than zero");
    }

    if (currency == null || !"EUR".equalsIgnoreCase(currency.trim())) {
      throw new IllegalArgumentException("Only EUR transfers are supported");
    }

    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new IllegalArgumentException("Idempotency key must not be blank");
    }

    return new Transfer(
        sourceAccountId,
        destinationAccountId,
        amount,
        currency.trim().toUpperCase(),
        idempotencyKey.trim());
  }

  /** Marks this transfer as successfully completed. */
  public void complete() {
    if (status != TransferStatus.PENDING) {
      throw new IllegalStateException("Only pending transfers can be completed");
    }

    status = TransferStatus.COMPLETED;
    failureReason = null;
    completedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  /**
   * Marks this transfer as failed.
   *
   * @param reason reason why the transfer failed
   */
  public void fail(String reason) {
    if (status != TransferStatus.PENDING) {
      throw new IllegalStateException("Only pending transfers can fail");
    }

    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("Failure reason must not be blank");
    }

    status = TransferStatus.FAILED;
    failureReason = reason.trim();
    completedAt = null;
  }

  /** Initializes creation and update timestamps before persistence. */
  @PrePersist
  protected void onCreate() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    createdAt = now;
    updatedAt = now;
  }

  /** Updates the modification timestamp before an entity update. */
  @PreUpdate
  protected void onUpdate() {
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
