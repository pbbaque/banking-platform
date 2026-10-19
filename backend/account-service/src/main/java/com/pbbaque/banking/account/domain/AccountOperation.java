package com.pbbaque.banking.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Represents an idempotent balance operation executed against an account.
 *
 * <p>The operation identifier is supplied by the caller and acts as the idempotency key. Reusing
 * the same identifier for the same logical operation must not modify the account balance again.
 */
@Entity
@Table(name = "ACCOUNT_OPERATIONS")
public class AccountOperation {

  @Id
  @JdbcTypeCode(SqlTypes.BINARY)
  @Column(name = "ID", nullable = false, updatable = false)
  private UUID id;

  @JdbcTypeCode(SqlTypes.BINARY)
  @Column(name = "ACCOUNT_ID", nullable = false, updatable = false)
  private UUID accountId;

  @Enumerated(EnumType.STRING)
  @Column(name = "OPERATION_TYPE", nullable = false, updatable = false, length = 20)
  private AccountOperationType operationType;

  @Column(name = "AMOUNT", nullable = false, updatable = false, precision = 19, scale = 4)
  private BigDecimal amount;

  @Column(
      name = "RESULTING_BALANCE",
      nullable = false,
      updatable = false,
      precision = 19,
      scale = 4)
  private BigDecimal resultingBalance;

  @Column(name = "CREATED_AT", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  protected AccountOperation() {}

  private AccountOperation(
      UUID id,
      UUID accountId,
      AccountOperationType operationType,
      BigDecimal amount,
      BigDecimal resultingBalance) {
    this.id = id;
    this.accountId = accountId;
    this.operationType = operationType;
    this.amount = amount;
    this.resultingBalance = resultingBalance;
  }

  /**
   * Creates a completed account balance operation.
   *
   * @param id unique operation identifier used as idempotency key
   * @param accountId identifier of the affected account
   * @param operationType type of operation
   * @param amount amount applied to the account
   * @param resultingBalance account balance after applying the operation
   * @return account operation
   */
  public static AccountOperation create(
      UUID id,
      UUID accountId,
      AccountOperationType operationType,
      BigDecimal amount,
      BigDecimal resultingBalance) {

    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(accountId, "accountId must not be null");
    Objects.requireNonNull(operationType, "operationType must not be null");
    Objects.requireNonNull(amount, "amount must not be null");
    Objects.requireNonNull(resultingBalance, "resultingBalance must not be null");

    if (amount.signum() <= 0) {
      throw new IllegalArgumentException("amount must be greater than zero");
    }

    if (amount.scale() > 4) {
      throw new IllegalArgumentException("amount cannot have more than 4 decimal places");
    }

    if (resultingBalance.signum() < 0) {
      throw new IllegalArgumentException("resultingBalance must not be negative");
    }

    if (resultingBalance.scale() > 4) {
      throw new IllegalArgumentException("resultingBalance cannot have more than 4 decimal places");
    }

    return new AccountOperation(id, accountId, operationType, amount, resultingBalance);
  }

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
  }

  /**
   * Returns the operation identifier.
   *
   * @return operation identifier
   */
  public UUID getId() {
    return id;
  }

  /**
   * Returns the affected account identifier.
   *
   * @return account identifier
   */
  public UUID getAccountId() {
    return accountId;
  }

  /**
   * Returns the operation type.
   *
   * @return operation type
   */
  public AccountOperationType getOperationType() {
    return operationType;
  }

  /**
   * Returns the operation amount.
   *
   * @return operation amount
   */
  public BigDecimal getAmount() {
    return amount;
  }

  /**
   * Returns the account balance immediately after the operation.
   *
   * @return resulting account balance
   */
  public BigDecimal getResultingBalance() {
    return resultingBalance;
  }

  /**
   * Returns when the operation was executed.
   *
   * @return creation timestamp
   */
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
