package com.pbbaque.banking.transfer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AccountOperationIdFactory}. */
class AccountOperationIdFactoryTest {

  private static final UUID TRANSFER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  private final AccountOperationIdFactory factory = new AccountOperationIdFactory();

  @Test
  void shouldGenerateStableDebitOperationId() {
    UUID first = factory.debit(TRANSFER_ID);
    UUID second = factory.debit(TRANSFER_ID);

    assertEquals(first, second);
  }

  @Test
  void shouldGenerateStableCreditOperationId() {
    UUID first = factory.credit(TRANSFER_ID);
    UUID second = factory.credit(TRANSFER_ID);

    assertEquals(first, second);
  }

  @Test
  void shouldGenerateStableCompensationOperationId() {
    UUID first = factory.compensation(TRANSFER_ID);
    UUID second = factory.compensation(TRANSFER_ID);

    assertEquals(first, second);
  }

  @Test
  void shouldGenerateDifferentIdsForDifferentOperationTypes() {
    UUID debit = factory.debit(TRANSFER_ID);
    UUID credit = factory.credit(TRANSFER_ID);
    UUID compensation = factory.compensation(TRANSFER_ID);

    assertNotEquals(debit, credit);
    assertNotEquals(debit, compensation);
    assertNotEquals(credit, compensation);
  }

  @Test
  void shouldGenerateDifferentIdsForDifferentTransfers() {
    UUID anotherTransferId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    assertNotEquals(factory.debit(TRANSFER_ID), factory.debit(anotherTransferId));
  }
}
