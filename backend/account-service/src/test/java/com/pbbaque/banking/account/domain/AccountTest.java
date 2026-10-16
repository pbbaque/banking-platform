package com.pbbaque.banking.account.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.pbbaque.banking.account.exception.AccountNotActiveException;
import com.pbbaque.banking.account.exception.InsufficientFundsException;
import com.pbbaque.banking.account.exception.InvalidAmountException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Account}. */
class AccountTest {

  private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  private static final String IBAN = "ES9121000418450200051332";

  @Test
  void shouldCreateActiveAccountWithZeroBalance() {
    Account account = Account.create(CUSTOMER_ID, IBAN, AccountType.CHECKING, "EUR");

    assertEquals(CUSTOMER_ID, account.getCustomerId());
    assertEquals(IBAN, account.getIban());
    assertEquals(AccountType.CHECKING, account.getAccountType());
    assertEquals("EUR", account.getCurrency());
    assertEquals(BigDecimal.ZERO, account.getBalance());
    assertEquals(AccountStatus.ACTIVE, account.getStatus());
  }

  @Test
  void shouldRejectBlankIban() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Account.create(CUSTOMER_ID, " ", AccountType.CHECKING, "EUR"));
  }

  @Test
  void shouldRejectUnsupportedCurrency() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Account.create(CUSTOMER_ID, IBAN, AccountType.CHECKING, "USD"));
  }

  @Test
  void shouldCreditAccount() {
    Account account = createAccount();

    account.credit(new BigDecimal("100.00"));

    assertEquals(new BigDecimal("100.00"), account.getBalance());
  }

  @Test
  void shouldDebitAccount() {
    Account account = createAccount();
    account.credit(new BigDecimal("100.00"));

    account.debit(new BigDecimal("25.00"));

    assertEquals(new BigDecimal("75.00"), account.getBalance());
  }

  @Test
  void shouldRejectDebitWhenFundsAreInsufficient() {
    Account account = createAccount();
    account.credit(new BigDecimal("50.00"));

    assertThrows(InsufficientFundsException.class, () -> account.debit(new BigDecimal("75.00")));
  }

  @Test
  void shouldRejectCreditWhenAccountIsBlocked() {
    Account account = createAccount();
    account.block();

    assertThrows(AccountNotActiveException.class, () -> account.credit(new BigDecimal("10.00")));
  }

  @Test
  void shouldRejectDebitWhenAccountIsBlocked() {
    Account account = createAccount();
    account.block();

    assertThrows(AccountNotActiveException.class, () -> account.debit(new BigDecimal("10.00")));
  }

  @Test
  void shouldBlockAccount() {
    Account account = createAccount();

    account.block();

    assertEquals(AccountStatus.BLOCKED, account.getStatus());
  }

  @Test
  void shouldActivateBlockedAccount() {
    Account account = createAccount();
    account.block();

    account.activate();

    assertEquals(AccountStatus.ACTIVE, account.getStatus());
  }

  @Test
  void shouldCloseAccountWithZeroBalance() {
    Account account = createAccount();

    account.close();

    assertEquals(AccountStatus.CLOSED, account.getStatus());
  }

  @Test
  void shouldRejectClosingAccountWithNonZeroBalance() {
    Account account = createAccount();
    account.credit(new BigDecimal("10.00"));

    assertThrows(IllegalStateException.class, account::close);
  }

  @Test
  void shouldRejectZeroAmount() {
    Account account = createAccount();

    assertThrows(InvalidAmountException.class, () -> account.credit(BigDecimal.ZERO));
  }

  @Test
  void shouldRejectNegativeAmount() {
    Account account = createAccount();

    assertThrows(InvalidAmountException.class, () -> account.credit(new BigDecimal("-1.00")));
  }

  @Test
  void shouldRejectAmountWithMoreThanFourDecimalPlaces() {
    Account account = createAccount();

    assertThrows(InvalidAmountException.class, () -> account.credit(new BigDecimal("1.00001")));
  }

  @Test
  void shouldRejectNullAmount() {
    Account account = createAccount();

    assertThrows(InvalidAmountException.class, () -> account.credit(null));
  }

  private Account createAccount() {
    return Account.create(CUSTOMER_ID, IBAN, AccountType.CHECKING, "EUR");
  }
}
