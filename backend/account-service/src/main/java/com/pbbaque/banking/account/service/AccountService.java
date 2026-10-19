package com.pbbaque.banking.account.service;

import com.pbbaque.banking.account.domain.Account;
import com.pbbaque.banking.account.domain.AccountOperation;
import com.pbbaque.banking.account.domain.AccountOperationType;
import com.pbbaque.banking.account.domain.AccountType;
import com.pbbaque.banking.account.exception.AccountAlreadyExistsException;
import com.pbbaque.banking.account.exception.AccountNotFoundException;
import com.pbbaque.banking.account.exception.IdempotencyConflictException;
import com.pbbaque.banking.account.repository.AccountOperationRepository;
import com.pbbaque.banking.account.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Provides account management operations. */
@Service
public class AccountService {

  private final AccountRepository accountRepository;
  private final AccountOperationRepository accountOperationRepository;

  /**
   * Creates a new account service.
   *
   * @param accountRepository account persistence repository
   * @param accountOperationRepository account operation persistence repository
   */
  public AccountService(
      AccountRepository accountRepository, AccountOperationRepository accountOperationRepository) {
    this.accountRepository = accountRepository;
    this.accountOperationRepository = accountOperationRepository;
  }

  /**
   * Creates a new banking account.
   *
   * @param customerId identifier of the customer who owns the account
   * @param iban account IBAN
   * @param accountType account type
   * @param currency account currency
   * @return created account
   * @throws AccountAlreadyExistsException when the IBAN is already registered
   */
  @Transactional
  public Account createAccount(
      UUID customerId, String iban, AccountType accountType, String currency) {

    if (accountRepository.existsByIban(iban)) {
      throw new AccountAlreadyExistsException("Account with this IBAN already exists");
    }

    Account account = Account.create(customerId, iban, accountType, currency);

    return accountRepository.save(account);
  }

  /**
   * Finds an account by its identifier.
   *
   * @param id account identifier
   * @return matching account
   * @throws AccountNotFoundException when the account does not exist
   */
  @Transactional(readOnly = true)
  public Account getById(UUID id) {
    return accountRepository
        .findById(id)
        .orElseThrow(() -> new AccountNotFoundException("Account not found"));
  }

  /**
   * Finds an account by its IBAN.
   *
   * @param iban account IBAN
   * @return matching account
   * @throws AccountNotFoundException when the account does not exist
   */
  @Transactional(readOnly = true)
  public Account getByIban(String iban) {
    return accountRepository
        .findByIban(iban)
        .orElseThrow(() -> new AccountNotFoundException("Account not found"));
  }

  /**
   * Finds all accounts belonging to a customer.
   *
   * @param customerId customer identifier
   * @return accounts belonging to the customer
   */
  @Transactional(readOnly = true)
  public List<Account> getByCustomerId(UUID customerId) {
    return accountRepository.findAllByCustomerId(customerId);
  }

  /**
   * Blocks an account.
   *
   * @param id account identifier
   * @return updated account
   * @throws AccountNotFoundException when the account does not exist
   */
  @Transactional
  public Account block(UUID id) {
    Account account = getById(id);
    account.block();
    return account;
  }

  /**
   * Activates an account.
   *
   * @param id account identifier
   * @return updated account
   * @throws AccountNotFoundException when the account does not exist
   */
  @Transactional
  public Account activate(UUID id) {
    Account account = getById(id);
    account.activate();
    return account;
  }

  /**
   * Closes an account.
   *
   * @param id account identifier
   * @return updated account
   * @throws AccountNotFoundException when the account does not exist
   */
  @Transactional
  public Account close(UUID id) {
    Account account = getById(id);
    account.close();
    return account;
  }

  /**
   * Credits funds to an account idempotently.
   *
   * <p>When the same operation identifier is received more than once with the same account, type
   * and amount, the account balance is modified only once.
   *
   * @param id account identifier
   * @param amount amount to credit
   * @param operationId unique identifier of the logical operation
   * @return updated account
   * @throws AccountNotFoundException when the account does not exist
   * @throws IdempotencyConflictException when the operation identifier was already used for a
   *     different operation
   */
  @Transactional
  public Account credit(UUID id, BigDecimal amount, UUID operationId) {
    return executeBalanceOperation(id, amount, operationId, AccountOperationType.CREDIT);
  }

  /**
   * Debits funds from an account idempotently.
   *
   * <p>When the same operation identifier is received more than once with the same account, type
   * and amount, the account balance is modified only once.
   *
   * @param id account identifier
   * @param amount amount to debit
   * @param operationId unique identifier of the logical operation
   * @return updated account
   * @throws AccountNotFoundException when the account does not exist
   * @throws IdempotencyConflictException when the operation identifier was already used for a
   *     different operation
   */
  @Transactional
  public Account debit(UUID id, BigDecimal amount, UUID operationId) {
    return executeBalanceOperation(id, amount, operationId, AccountOperationType.DEBIT);
  }

  private Account executeBalanceOperation(
      UUID accountId, BigDecimal amount, UUID operationId, AccountOperationType operationType) {

    AccountOperation existingOperation =
        accountOperationRepository.findById(operationId).orElse(null);

    if (existingOperation != null) {
      validateReplay(existingOperation, accountId, amount, operationType);
      return getById(accountId);
    }

    Account account = getById(accountId);

    if (operationType == AccountOperationType.CREDIT) {
      account.credit(amount);
    } else {
      account.debit(amount);
    }

    AccountOperation operation =
        AccountOperation.create(
            operationId, accountId, operationType, amount, account.getBalance());

    accountOperationRepository.save(operation);

    return account;
  }

  private void validateReplay(
      AccountOperation existingOperation,
      UUID accountId,
      BigDecimal amount,
      AccountOperationType operationType) {

    boolean sameAccount = existingOperation.getAccountId().equals(accountId);
    boolean sameType = existingOperation.getOperationType() == operationType;
    boolean sameAmount = existingOperation.getAmount().compareTo(amount) == 0;

    if (!sameAccount || !sameType || !sameAmount) {
      throw new IdempotencyConflictException(
          "Idempotency key was already used for a different account operation");
    }
  }
}
