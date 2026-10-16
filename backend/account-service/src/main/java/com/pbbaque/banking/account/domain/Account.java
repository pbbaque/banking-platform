package com.pbbaque.banking.account.domain;

import com.pbbaque.banking.account.exception.AccountNotActiveException;
import com.pbbaque.banking.account.exception.InsufficientFundsException;
import com.pbbaque.banking.account.exception.InvalidAmountException;
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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Represents a banking account owned by a customer. */
@Entity
@Table(name = "ACCOUNTS")
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @JdbcTypeCode(SqlTypes.BINARY)
  @Column(name = "ID", nullable = false, updatable = false)
  private UUID id;

  @JdbcTypeCode(SqlTypes.BINARY)
  @Column(name = "CUSTOMER_ID", nullable = false, updatable = false)
  private UUID customerId;

  @Column(name = "IBAN", nullable = false, unique = true, length = 34)
  private String iban;

  @Enumerated(EnumType.STRING)
  @Column(name = "ACCOUNT_TYPE", nullable = false, length = 30)
  private AccountType accountType;

  @Column(name = "CURRENCY", nullable = false, length = 3)
  private String currency;

  @Column(name = "BALANCE", nullable = false, precision = 19, scale = 4)
  private BigDecimal balance;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS", nullable = false, length = 30)
  private AccountStatus status;

  @Version
  @Column(name = "VERSION", nullable = false)
  private Long version;

  @Column(name = "CREATED_AT", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "UPDATED_AT", nullable = false)
  private OffsetDateTime updatedAt;

  @Column(name = "CLOSED_AT")
  private OffsetDateTime closedAt;

  protected Account() {}

  private Account(
      UUID customerId,
      String iban,
      AccountType accountType,
      String currency,
      BigDecimal balance,
      AccountStatus status) {
    this.customerId = customerId;
    this.iban = iban;
    this.accountType = accountType;
    this.currency = currency;
    this.balance = balance;
    this.status = status;
  }

  /**
   * Creates a new active banking account with zero balance.
   *
   * @param customerId identifier of the customer who owns the account
   * @param iban account IBAN
   * @param accountType type of banking account
   * @param currency account currency
   * @return newly created account
   */
  public static Account create(
      UUID customerId, String iban, AccountType accountType, String currency) {
    Objects.requireNonNull(customerId, "customerId must not be null");
    Objects.requireNonNull(iban, "iban must not be null");
    Objects.requireNonNull(accountType, "accountType must not be null");
    Objects.requireNonNull(currency, "currency must not be null");

    if (iban.isBlank()) {
      throw new IllegalArgumentException("iban must not be blank");
    }

    if (!"EUR".equals(currency)) {
      throw new IllegalArgumentException("Only EUR accounts are currently supported");
    }

    return new Account(
        customerId, iban, accountType, currency, BigDecimal.ZERO, AccountStatus.ACTIVE);
  }

  @PrePersist
  void onCreate() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  /**
   * Returns the account identifier.
   *
   * @return account identifier
   */
  public UUID getId() {
    return id;
  }

  /**
   * Returns the identifier of the customer who owns the account.
   *
   * @return customer identifier
   */
  public UUID getCustomerId() {
    return customerId;
  }

  /**
   * Returns the account IBAN.
   *
   * @return account IBAN
   */
  public String getIban() {
    return iban;
  }

  /**
   * Returns the account type.
   *
   * @return account type
   */
  public AccountType getAccountType() {
    return accountType;
  }

  /**
   * Returns the account currency.
   *
   * @return account currency
   */
  public String getCurrency() {
    return currency;
  }

  /**
   * Returns the current account balance.
   *
   * @return account balance
   */
  public BigDecimal getBalance() {
    return balance;
  }

  /**
   * Returns the current account status.
   *
   * @return account status
   */
  public AccountStatus getStatus() {
    return status;
  }

  /**
   * Returns the optimistic locking version.
   *
   * @return entity version
   */
  public Long getVersion() {
    return version;
  }

  /**
   * Returns the account creation timestamp.
   *
   * @return creation timestamp
   */
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  /**
   * Returns the timestamp of the last account update.
   *
   * @return last update timestamp
   */
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Returns the timestamp when the account was closed.
   *
   * @return closing timestamp, or {@code null} when the account is not closed
   */
  public OffsetDateTime getClosedAt() {
    return closedAt;
  }

  /** Blocks the account and prevents normal account operations. */
  public void block() {
    if (status == AccountStatus.CLOSED) {
      throw new IllegalStateException("Closed account cannot be blocked");
    }

    status = AccountStatus.BLOCKED;
  }

  /** Activates the account. */
  public void activate() {
    if (status == AccountStatus.CLOSED) {
      throw new IllegalStateException("Closed account cannot be activated");
    }

    status = AccountStatus.ACTIVE;
  }

  /**
   * Closes the account.
   *
   * <p>The account can only be closed when its balance is zero.
   */
  public void close() {
    if (status == AccountStatus.CLOSED) {
      throw new IllegalStateException("Account is already closed");
    }

    if (balance.compareTo(BigDecimal.ZERO) != 0) {
      throw new IllegalStateException("Account with non-zero balance cannot be closed");
    }

    status = AccountStatus.CLOSED;
    closedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  /**
   * Credits funds to the account.
   *
   * @param amount amount to credit
   * @throws InvalidAmountException when the amount is invalid
   * @throws AccountNotActiveException when the account is not active
   */
  public void credit(BigDecimal amount) {
    validateAmount(amount);

    if (status != AccountStatus.ACTIVE) {
      throw new AccountNotActiveException("Only active accounts can receive funds");
    }

    balance = balance.add(amount);
  }

  /**
   * Debits funds from the account.
   *
   * @param amount amount to debit
   * @throws InvalidAmountException when the amount is invalid
   * @throws AccountNotActiveException when the account is not active
   * @throws InsufficientFundsException when the account does not have enough balance
   */
  public void debit(BigDecimal amount) {
    validateAmount(amount);

    if (status != AccountStatus.ACTIVE) {
      throw new AccountNotActiveException("Only active accounts can send funds");
    }

    if (balance.compareTo(amount) < 0) {
      throw new InsufficientFundsException("Insufficient funds");
    }

    balance = balance.subtract(amount);
  }

  private void validateAmount(BigDecimal amount) {
    if (amount == null) {
      throw new InvalidAmountException("amount must not be null");
    }

    if (amount.signum() <= 0) {
      throw new InvalidAmountException("amount must be greater than zero");
    }

    if (amount.scale() > 4) {
      throw new InvalidAmountException("amount cannot have more than 4 decimal places");
    }
  }
}
