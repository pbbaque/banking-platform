package com.pbbaque.banking.account.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AccountOperation}. */
class AccountOperationTest {

  private static final UUID OPERATION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  private static final UUID ACCOUNT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  @Test
  void shouldCreateAccountOperation() {
    AccountOperation operation =
        AccountOperation.create(
            OPERATION_ID,
            ACCOUNT_ID,
            AccountOperationType.DEBIT,
            new BigDecimal("25.00"),
            new BigDecimal("75.00"));

    assertEquals(OPERATION_ID, operation.getId());
    assertEquals(ACCOUNT_ID, operation.getAccountId());
    assertEquals(AccountOperationType.DEBIT, operation.getOperationType());
    assertEquals(new BigDecimal("25.00"), operation.getAmount());
    assertEquals(new BigDecimal("75.00"), operation.getResultingBalance());
  }

  @Test
  void shouldRejectNullOperationId() {
    assertThrows(
        NullPointerException.class,
        () ->
            AccountOperation.create(
                null,
                ACCOUNT_ID,
                AccountOperationType.DEBIT,
                new BigDecimal("25.00"),
                new BigDecimal("75.00")));
  }

  @Test
  void shouldRejectNullAccountId() {
    assertThrows(
        NullPointerException.class,
        () ->
            AccountOperation.create(
                OPERATION_ID,
                null,
                AccountOperationType.DEBIT,
                new BigDecimal("25.00"),
                new BigDecimal("75.00")));
  }

  @Test
  void shouldRejectNullOperationType() {
    assertThrows(
        NullPointerException.class,
        () ->
            AccountOperation.create(
                OPERATION_ID, ACCOUNT_ID, null, new BigDecimal("25.00"), new BigDecimal("75.00")));
  }

  @Test
  void shouldRejectNullAmount() {
    assertThrows(
        NullPointerException.class,
        () ->
            AccountOperation.create(
                OPERATION_ID,
                ACCOUNT_ID,
                AccountOperationType.DEBIT,
                null,
                new BigDecimal("75.00")));
  }

  @Test
  void shouldRejectZeroAmount() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            AccountOperation.create(
                OPERATION_ID,
                ACCOUNT_ID,
                AccountOperationType.DEBIT,
                BigDecimal.ZERO,
                new BigDecimal("75.00")));
  }

  @Test
  void shouldRejectNegativeAmount() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            AccountOperation.create(
                OPERATION_ID,
                ACCOUNT_ID,
                AccountOperationType.DEBIT,
                new BigDecimal("-1.00"),
                new BigDecimal("75.00")));
  }

  @Test
  void shouldRejectAmountWithMoreThanFourDecimalPlaces() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            AccountOperation.create(
                OPERATION_ID,
                ACCOUNT_ID,
                AccountOperationType.DEBIT,
                new BigDecimal("1.00001"),
                new BigDecimal("75.00")));
  }

  @Test
  void shouldRejectNullResultingBalance() {
    assertThrows(
        NullPointerException.class,
        () ->
            AccountOperation.create(
                OPERATION_ID,
                ACCOUNT_ID,
                AccountOperationType.DEBIT,
                new BigDecimal("25.00"),
                null));
  }

  @Test
  void shouldRejectNegativeResultingBalance() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            AccountOperation.create(
                OPERATION_ID,
                ACCOUNT_ID,
                AccountOperationType.DEBIT,
                new BigDecimal("25.00"),
                new BigDecimal("-0.0001")));
  }

  @Test
  void shouldRejectResultingBalanceWithMoreThanFourDecimalPlaces() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            AccountOperation.create(
                OPERATION_ID,
                ACCOUNT_ID,
                AccountOperationType.CREDIT,
                new BigDecimal("25.00"),
                new BigDecimal("100.00001")));
  }
}
