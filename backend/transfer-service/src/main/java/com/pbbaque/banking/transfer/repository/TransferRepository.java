package com.pbbaque.banking.transfer.repository;

import com.pbbaque.banking.transfer.domain.Transfer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for banking transfers. */
public interface TransferRepository extends JpaRepository<Transfer, UUID> {

  /**
   * Finds a transfer by its idempotency key.
   *
   * @param idempotencyKey idempotency key
   * @return transfer when found
   */
  Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

  /**
   * Returns all transfers sent from the specified account.
   *
   * @param sourceAccountId source account identifier
   * @return transfers sent from the account
   */
  List<Transfer> findAllBySourceAccountId(UUID sourceAccountId);

  /**
   * Returns all transfers received by the specified account.
   *
   * @param destinationAccountId destination account identifier
   * @return transfers received by the account
   */
  List<Transfer> findAllByDestinationAccountId(UUID destinationAccountId);

  /**
   * Returns all transfers sent from or received by the specified account, newest first.
   *
   * @param sourceAccountId source account identifier
   * @param destinationAccountId destination account identifier
   * @return account transfer history
   */
  List<Transfer> findAllBySourceAccountIdOrDestinationAccountIdOrderByCreatedAtDesc(
      UUID sourceAccountId, UUID destinationAccountId);

  /**
   * Checks whether an idempotency key already exists.
   *
   * @param idempotencyKey idempotency key
   * @return true when the key already exists
   */
  boolean existsByIdempotencyKey(String idempotencyKey);
}
