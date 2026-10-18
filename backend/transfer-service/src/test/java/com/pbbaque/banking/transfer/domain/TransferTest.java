package com.pbbaque.banking.transfer.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Transfer}. */
class TransferTest {

  private static final UUID SOURCE_ACCOUNT_ID =
      UUID.fromString("11111111-1111-1111-1111-111111111111");

  private static final UUID DESTINATION_ACCOUNT_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");

  @Test
  void shouldCreatePendingTransfer() {
    Transfer transfer =
        Transfer.create(
            SOURCE_ACCOUNT_ID,
            DESTINATION_ACCOUNT_ID,
            new BigDecimal("100.0000"),
            "EUR",
            "transfer-key-1");

    assertEquals(SOURCE_ACCOUNT_ID, transfer.getSourceAccountId());
    assertEquals(DESTINATION_ACCOUNT_ID, transfer.getDestinationAccountId());
    assertEquals(new BigDecimal("100.0000"), transfer.getAmount());
    assertEquals("EUR", transfer.getCurrency());
    assertEquals("transfer-key-1", transfer.getIdempotencyKey());
    assertEquals(TransferStatus.PENDING, transfer.getStatus());
    assertNull(transfer.getFailureReason());
    assertNull(transfer.getCompletedAt());
  }

  @Test
  void shouldNormalizeCurrencyAndIdempotencyKey() {
    Transfer transfer =
        Transfer.create(
            SOURCE_ACCOUNT_ID,
            DESTINATION_ACCOUNT_ID,
            new BigDecimal("50.0000"),
            " eur ",
            "  transfer-key-2  ");

    assertEquals("EUR", transfer.getCurrency());
    assertEquals("transfer-key-2", transfer.getIdempotencyKey());
  }

  @Test
  void shouldCompletePendingTransfer() {
    Transfer transfer = createTransfer();

    transfer.complete();

    assertEquals(TransferStatus.COMPLETED, transfer.getStatus());
    assertNull(transfer.getFailureReason());
  }

  @Test
  void shouldFailPendingTransfer() {
    Transfer transfer = createTransfer();

    transfer.fail("Destination credit failed");

    assertEquals(TransferStatus.FAILED, transfer.getStatus());
    assertEquals("Destination credit failed", transfer.getFailureReason());
    assertNull(transfer.getCompletedAt());
  }

  @Test
  void shouldRejectNullSourceAccount() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Transfer.create(null, DESTINATION_ACCOUNT_ID, new BigDecimal("10.0000"), "EUR", "key"));
  }

  @Test
  void shouldRejectNullDestinationAccount() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Transfer.create(SOURCE_ACCOUNT_ID, null, new BigDecimal("10.0000"), "EUR", "key"));
  }

  @Test
  void shouldRejectSameSourceAndDestinationAccount() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Transfer.create(
                SOURCE_ACCOUNT_ID, SOURCE_ACCOUNT_ID, new BigDecimal("10.0000"), "EUR", "key"));
  }

  @Test
  void shouldRejectNullAmount() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Transfer.create(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, null, "EUR", "key"));
  }

  @Test
  void shouldRejectZeroAmount() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Transfer.create(
                SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, BigDecimal.ZERO, "EUR", "key"));
  }

  @Test
  void shouldRejectNegativeAmount() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Transfer.create(
                SOURCE_ACCOUNT_ID,
                DESTINATION_ACCOUNT_ID,
                new BigDecimal("-1.0000"),
                "EUR",
                "key"));
  }

  @Test
  void shouldRejectUnsupportedCurrency() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Transfer.create(
                SOURCE_ACCOUNT_ID,
                DESTINATION_ACCOUNT_ID,
                new BigDecimal("10.0000"),
                "USD",
                "key"));
  }

  @Test
  void shouldRejectBlankIdempotencyKey() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Transfer.create(
                SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, new BigDecimal("10.0000"), "EUR", " "));
  }

  @Test
  void shouldRejectCompletingNonPendingTransfer() {
    Transfer transfer = createTransfer();
    transfer.complete();

    assertThrows(IllegalStateException.class, transfer::complete);
  }

  @Test
  void shouldRejectFailingNonPendingTransfer() {
    Transfer transfer = createTransfer();
    transfer.complete();

    assertThrows(IllegalStateException.class, () -> transfer.fail("Failure"));
  }

  @Test
  void shouldRejectBlankFailureReason() {
    Transfer transfer = createTransfer();

    assertThrows(IllegalArgumentException.class, () -> transfer.fail(" "));
  }

  private Transfer createTransfer() {
    return Transfer.create(
        SOURCE_ACCOUNT_ID,
        DESTINATION_ACCOUNT_ID,
        new BigDecimal("100.0000"),
        "EUR",
        "transfer-key");
  }
}
