package com.pbbaque.banking.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pbbaque.banking.account.domain.Account;
import com.pbbaque.banking.account.domain.AccountStatus;
import com.pbbaque.banking.account.domain.AccountType;
import com.pbbaque.banking.account.exception.AccountAlreadyExistsException;
import com.pbbaque.banking.account.exception.AccountNotFoundException;
import com.pbbaque.banking.account.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link AccountService}. */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  private static final UUID ACCOUNT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  private static final String IBAN = "ES9121000418450200051332";

  @Mock private AccountRepository accountRepository;

  private AccountService accountService;

  @BeforeEach
  void setUp() {
    accountService = new AccountService(accountRepository);
  }

  @Test
  void shouldCreateAccount() {
    when(accountRepository.existsByIban(IBAN)).thenReturn(false);
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Account account = accountService.createAccount(CUSTOMER_ID, IBAN, AccountType.CHECKING, "EUR");

    assertEquals(CUSTOMER_ID, account.getCustomerId());
    assertEquals(IBAN, account.getIban());
    assertEquals(AccountType.CHECKING, account.getAccountType());
    assertEquals("EUR", account.getCurrency());
    assertEquals(BigDecimal.ZERO, account.getBalance());
    assertEquals(AccountStatus.ACTIVE, account.getStatus());

    verify(accountRepository).existsByIban(IBAN);
    verify(accountRepository).save(any(Account.class));
  }

  @Test
  void shouldRejectDuplicateIban() {
    when(accountRepository.existsByIban(IBAN)).thenReturn(true);

    assertThrows(
        AccountAlreadyExistsException.class,
        () -> accountService.createAccount(CUSTOMER_ID, IBAN, AccountType.CHECKING, "EUR"));

    verify(accountRepository).existsByIban(IBAN);
    verify(accountRepository, never()).save(any(Account.class));
  }

  @Test
  void shouldFindAccountById() {
    Account account = createAccount();
    when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

    Account result = accountService.getById(ACCOUNT_ID);

    assertSame(account, result);
    verify(accountRepository).findById(ACCOUNT_ID);
  }

  @Test
  void shouldThrowWhenAccountByIdDoesNotExist() {
    when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

    assertThrows(AccountNotFoundException.class, () -> accountService.getById(ACCOUNT_ID));

    verify(accountRepository).findById(ACCOUNT_ID);
  }

  @Test
  void shouldFindAccountByIban() {
    Account account = createAccount();
    when(accountRepository.findByIban(IBAN)).thenReturn(Optional.of(account));

    Account result = accountService.getByIban(IBAN);

    assertSame(account, result);
    verify(accountRepository).findByIban(IBAN);
  }

  @Test
  void shouldThrowWhenAccountByIbanDoesNotExist() {
    when(accountRepository.findByIban(IBAN)).thenReturn(Optional.empty());

    assertThrows(AccountNotFoundException.class, () -> accountService.getByIban(IBAN));

    verify(accountRepository).findByIban(IBAN);
  }

  @Test
  void shouldFindAccountsByCustomerId() {
    Account first = createAccount();
    Account second =
        Account.create(CUSTOMER_ID, "ES7921000813610123456789", AccountType.SAVINGS, "EUR");

    when(accountRepository.findAllByCustomerId(CUSTOMER_ID)).thenReturn(List.of(first, second));

    List<Account> result = accountService.getByCustomerId(CUSTOMER_ID);

    assertEquals(2, result.size());
    verify(accountRepository).findAllByCustomerId(CUSTOMER_ID);
  }

  @Test
  void shouldBlockAccount() {
    Account account = createAccount();
    when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

    Account result = accountService.block(ACCOUNT_ID);

    assertSame(account, result);
    assertEquals(AccountStatus.BLOCKED, result.getStatus());
  }

  @Test
  void shouldActivateAccount() {
    Account account = createAccount();
    account.block();
    when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

    Account result = accountService.activate(ACCOUNT_ID);

    assertEquals(AccountStatus.ACTIVE, result.getStatus());
  }

  @Test
  void shouldCloseAccount() {
    Account account = createAccount();
    when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

    Account result = accountService.close(ACCOUNT_ID);

    assertEquals(AccountStatus.CLOSED, result.getStatus());
  }

  @Test
  void shouldCreditAccount() {
    Account account = createAccount();
    when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

    Account result = accountService.credit(ACCOUNT_ID, new BigDecimal("100.00"));

    assertEquals(new BigDecimal("100.00"), result.getBalance());
  }

  @Test
  void shouldDebitAccount() {
    Account account = createAccount();
    account.credit(new BigDecimal("100.00"));
    when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

    Account result = accountService.debit(ACCOUNT_ID, new BigDecimal("25.00"));

    assertEquals(new BigDecimal("75.00"), result.getBalance());
  }

  private Account createAccount() {
    return Account.create(CUSTOMER_ID, IBAN, AccountType.CHECKING, "EUR");
  }
}
