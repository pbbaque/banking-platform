package com.pbbaque.banking.account.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pbbaque.banking.account.domain.Account;
import com.pbbaque.banking.account.domain.AccountStatus;
import com.pbbaque.banking.account.domain.AccountType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/** Integration tests for {@link AccountRepository}. */
@DataJpaTest
@ActiveProfiles("local")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountRepositoryIT {
  private static final UUID CUSTOMER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

  private static final UUID SECOND_CUSTOMER_ID =
      UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final String IBAN = "ES1111111111111111111111";

  private static final String SECOND_IBAN = "ES2222222222222222222222";

  private static final String THIRD_IBAN = "ES3333333333333333333333";

  @Autowired private AccountRepository accountRepository;

  @Test
  void shouldPersistAccount() {
    Account account = Account.create(CUSTOMER_ID, IBAN, AccountType.CHECKING, "EUR");

    Account saved = accountRepository.saveAndFlush(account);

    assertNotNull(saved.getId());
    assertEquals(CUSTOMER_ID, saved.getCustomerId());
    assertEquals(IBAN, saved.getIban());
    assertEquals(AccountType.CHECKING, saved.getAccountType());
    assertEquals(AccountStatus.ACTIVE, saved.getStatus());
    assertEquals(BigDecimal.ZERO, saved.getBalance());
    assertNotNull(saved.getCreatedAt());
    assertNotNull(saved.getUpdatedAt());
  }

  @Test
  void shouldFindAccountByIban() {
    Account account = Account.create(CUSTOMER_ID, IBAN, AccountType.CHECKING, "EUR");

    accountRepository.saveAndFlush(account);

    Optional<Account> result = accountRepository.findByIban(IBAN);

    assertTrue(result.isPresent());
    assertEquals(IBAN, result.get().getIban());
  }

  @Test
  void shouldReturnEmptyWhenIbanDoesNotExist() {
    Optional<Account> result = accountRepository.findByIban("ES0000000000000000000000");

    assertTrue(result.isEmpty());
  }

  @Test
  void shouldFindAllAccountsByCustomerId() {
    Account checking = Account.create(CUSTOMER_ID, IBAN, AccountType.CHECKING, "EUR");

    Account savings = Account.create(CUSTOMER_ID, SECOND_IBAN, AccountType.SAVINGS, "EUR");

    Account otherCustomer =
        Account.create(SECOND_CUSTOMER_ID, THIRD_IBAN, AccountType.CHECKING, "EUR");

    accountRepository.save(checking);
    accountRepository.save(savings);
    accountRepository.save(otherCustomer);
    accountRepository.flush();

    List<Account> result = accountRepository.findAllByCustomerId(CUSTOMER_ID);

    assertEquals(2, result.size());
    assertTrue(result.stream().allMatch(account -> CUSTOMER_ID.equals(account.getCustomerId())));
  }

  @Test
  void shouldDetectExistingIban() {
    Account account = Account.create(CUSTOMER_ID, IBAN, AccountType.CHECKING, "EUR");

    accountRepository.saveAndFlush(account);

    assertTrue(accountRepository.existsByIban(IBAN));
    assertFalse(accountRepository.existsByIban("ES0000000000000000000000"));
  }

  @Test
  void shouldPersistBalanceUpdate() {
    Account account = Account.create(CUSTOMER_ID, IBAN, AccountType.CHECKING, "EUR");

    Account saved = accountRepository.saveAndFlush(account);

    saved.credit(new BigDecimal("100.00"));
    accountRepository.flush();

    Account reloaded = accountRepository.findById(saved.getId()).orElseThrow();

    assertEquals(new BigDecimal("100.00"), reloaded.getBalance());
  }

  @Test
  void shouldPersistStatusUpdate() {
    Account account = Account.create(CUSTOMER_ID, IBAN, AccountType.CHECKING, "EUR");

    Account saved = accountRepository.saveAndFlush(account);

    saved.block();
    accountRepository.flush();

    Account reloaded = accountRepository.findById(saved.getId()).orElseThrow();

    assertEquals(AccountStatus.BLOCKED, reloaded.getStatus());
  }

  @Test
  void shouldIncrementVersionWhenAccountIsUpdated() {
    Account account = Account.create(CUSTOMER_ID, IBAN, AccountType.CHECKING, "EUR");

    Account saved = accountRepository.saveAndFlush(account);

    Long initialVersion = saved.getVersion();

    saved.credit(new BigDecimal("10.00"));
    accountRepository.flush();

    assertEquals(initialVersion + 1, saved.getVersion());
  }
}
