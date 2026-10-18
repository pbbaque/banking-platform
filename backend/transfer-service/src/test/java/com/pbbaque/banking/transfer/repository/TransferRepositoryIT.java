package com.pbbaque.banking.transfer.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pbbaque.banking.transfer.domain.Transfer;
import com.pbbaque.banking.transfer.domain.TransferStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/** Integration tests for {@link TransferRepository}. */
@DataJpaTest
@ActiveProfiles("local")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransferRepositoryIT {

  private static final UUID SOURCE_ACCOUNT_ID =
      UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

  private static final UUID DESTINATION_ACCOUNT_ID =
      UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

  private static final UUID SECOND_SOURCE_ACCOUNT_ID =
      UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

  @Autowired private TransferRepository transferRepository;

  @Test
  void shouldPersistTransfer() {
    Transfer transfer =
        Transfer.create(
            SOURCE_ACCOUNT_ID,
            DESTINATION_ACCOUNT_ID,
            new BigDecimal("100.0000"),
            "EUR",
            "repo-it-key-1");

    Transfer saved = transferRepository.saveAndFlush(transfer);

    assertTrue(saved.getId() != null);
    assertEquals(TransferStatus.PENDING, saved.getStatus());
    assertEquals(SOURCE_ACCOUNT_ID, saved.getSourceAccountId());
    assertEquals(DESTINATION_ACCOUNT_ID, saved.getDestinationAccountId());
  }

  @Test
  void shouldFindByIdempotencyKey() {
    Transfer transfer =
        Transfer.create(
            SOURCE_ACCOUNT_ID,
            DESTINATION_ACCOUNT_ID,
            new BigDecimal("50.0000"),
            "EUR",
            "repo-it-key-2");

    transferRepository.saveAndFlush(transfer);

    Transfer found = transferRepository.findByIdempotencyKey("repo-it-key-2").orElseThrow();

    assertEquals("repo-it-key-2", found.getIdempotencyKey());
    assertEquals(new BigDecimal("50.0000"), found.getAmount());
  }

  @Test
  void shouldReturnEmptyWhenIdempotencyKeyDoesNotExist() {
    assertTrue(transferRepository.findByIdempotencyKey("missing-key").isEmpty());
  }

  @Test
  void shouldFindTransfersBySourceAccount() {
    transferRepository.saveAndFlush(
        Transfer.create(
            SOURCE_ACCOUNT_ID,
            DESTINATION_ACCOUNT_ID,
            new BigDecimal("10.0000"),
            "EUR",
            "repo-it-key-3"));

    transferRepository.saveAndFlush(
        Transfer.create(
            SOURCE_ACCOUNT_ID,
            SECOND_SOURCE_ACCOUNT_ID,
            new BigDecimal("20.0000"),
            "EUR",
            "repo-it-key-4"));

    transferRepository.saveAndFlush(
        Transfer.create(
            SECOND_SOURCE_ACCOUNT_ID,
            DESTINATION_ACCOUNT_ID,
            new BigDecimal("30.0000"),
            "EUR",
            "repo-it-key-5"));

    List<Transfer> transfers = transferRepository.findAllBySourceAccountId(SOURCE_ACCOUNT_ID);

    assertEquals(2, transfers.size());
  }

  @Test
  void shouldFindTransfersByDestinationAccount() {
    transferRepository.saveAndFlush(
        Transfer.create(
            SOURCE_ACCOUNT_ID,
            DESTINATION_ACCOUNT_ID,
            new BigDecimal("10.0000"),
            "EUR",
            "repo-it-key-6"));

    transferRepository.saveAndFlush(
        Transfer.create(
            SECOND_SOURCE_ACCOUNT_ID,
            DESTINATION_ACCOUNT_ID,
            new BigDecimal("20.0000"),
            "EUR",
            "repo-it-key-7"));

    List<Transfer> transfers =
        transferRepository.findAllByDestinationAccountId(DESTINATION_ACCOUNT_ID);

    assertEquals(2, transfers.size());
  }

  @Test
  void shouldCheckExistingIdempotencyKey() {
    transferRepository.saveAndFlush(
        Transfer.create(
            SOURCE_ACCOUNT_ID,
            DESTINATION_ACCOUNT_ID,
            new BigDecimal("40.0000"),
            "EUR",
            "repo-it-key-8"));

    assertTrue(transferRepository.existsByIdempotencyKey("repo-it-key-8"));

    assertFalse(transferRepository.existsByIdempotencyKey("missing-key"));
  }

  @Test
  void shouldPersistCompletedTransfer() {
    Transfer transfer =
        Transfer.create(
            SOURCE_ACCOUNT_ID,
            DESTINATION_ACCOUNT_ID,
            new BigDecimal("60.0000"),
            "EUR",
            "repo-it-key-9");

    transfer.complete();

    Transfer saved = transferRepository.saveAndFlush(transfer);

    assertEquals(TransferStatus.COMPLETED, saved.getStatus());
    assertTrue(saved.getCompletedAt() != null);
  }

  @Test
  void shouldPersistFailedTransfer() {
    Transfer transfer =
        Transfer.create(
            SOURCE_ACCOUNT_ID,
            DESTINATION_ACCOUNT_ID,
            new BigDecimal("70.0000"),
            "EUR",
            "repo-it-key-10");

    transfer.fail("Integration test failure");

    Transfer saved = transferRepository.saveAndFlush(transfer);

    assertEquals(TransferStatus.FAILED, saved.getStatus());
    assertEquals("Integration test failure", saved.getFailureReason());
  }
}
