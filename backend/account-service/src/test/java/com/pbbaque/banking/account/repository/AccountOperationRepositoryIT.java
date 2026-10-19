package com.pbbaque.banking.account.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pbbaque.banking.account.domain.Account;
import com.pbbaque.banking.account.domain.AccountOperation;
import com.pbbaque.banking.account.domain.AccountOperationType;
import com.pbbaque.banking.account.domain.AccountType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/** Integration tests for {@link AccountOperationRepository}. */
@DataJpaTest
@ActiveProfiles("local")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountOperationRepositoryIT {

  private static final UUID CUSTOMER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

  private static final UUID OPERATION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

  private static final String IBAN = "ES4444444444444444444444";

  @Autowired private AccountRepository accountRepository;

  @Autowired private AccountOperationRepository accountOperationRepository;

  @Autowired private EntityManager entityManager;

  @Test
  void shouldPersistAccountOperation() {
    Account account = createAndPersistAccount();

    AccountOperation operation =
        AccountOperation.create(
            OPERATION_ID,
            account.getId(),
            AccountOperationType.DEBIT,
            new BigDecimal("25.00"),
            new BigDecimal("75.00"));

    AccountOperation saved = accountOperationRepository.saveAndFlush(operation);

    assertEquals(OPERATION_ID, saved.getId());
    assertEquals(account.getId(), saved.getAccountId());
    assertEquals(AccountOperationType.DEBIT, saved.getOperationType());
    assertEquals(new BigDecimal("25.00"), saved.getAmount());
    assertEquals(new BigDecimal("75.00"), saved.getResultingBalance());
    assertNotNull(saved.getCreatedAt());
  }

  @Test
  void shouldFindAccountOperationById() {
    Account account = createAndPersistAccount();

    AccountOperation operation =
        AccountOperation.create(
            OPERATION_ID,
            account.getId(),
            AccountOperationType.CREDIT,
            new BigDecimal("50.00"),
            new BigDecimal("50.00"));

    accountOperationRepository.saveAndFlush(operation);

    Optional<AccountOperation> result = accountOperationRepository.findById(OPERATION_ID);

    assertTrue(result.isPresent());
    assertEquals(OPERATION_ID, result.get().getId());
    assertEquals(account.getId(), result.get().getAccountId());
    assertEquals(AccountOperationType.CREDIT, result.get().getOperationType());
  }

  @Test
  void shouldRejectDuplicateOperationId() {
    Account account = createAndPersistAccount();

    AccountOperation first =
        AccountOperation.create(
            OPERATION_ID,
            account.getId(),
            AccountOperationType.CREDIT,
            new BigDecimal("25.00"),
            new BigDecimal("25.00"));

    entityManager.persist(first);
    entityManager.flush();
    entityManager.clear();

    AccountOperation duplicate =
        AccountOperation.create(
            OPERATION_ID,
            account.getId(),
            AccountOperationType.CREDIT,
            new BigDecimal("25.00"),
            new BigDecimal("25.00"));

    assertThrows(
        PersistenceException.class,
        () -> {
          entityManager.persist(duplicate);
          entityManager.flush();
        });
  }

  @Test
  void shouldRejectOperationForNonExistingAccount() {
    UUID unknownAccountId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    AccountOperation operation =
        AccountOperation.create(
            OPERATION_ID,
            unknownAccountId,
            AccountOperationType.DEBIT,
            new BigDecimal("25.00"),
            new BigDecimal("75.00"));

    assertThrows(
        DataIntegrityViolationException.class,
        () -> accountOperationRepository.saveAndFlush(operation));
  }

  private Account createAndPersistAccount() {
    Account account = Account.create(CUSTOMER_ID, IBAN, AccountType.CHECKING, "EUR");
    return accountRepository.saveAndFlush(account);
  }
}
