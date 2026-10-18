package com.pbbaque.banking.transfer.service;

import com.pbbaque.banking.transfer.client.AccountClient;
import com.pbbaque.banking.transfer.domain.Transfer;
import com.pbbaque.banking.transfer.dto.CreateTransferRequest;
import com.pbbaque.banking.transfer.exception.AccountServiceException;
import com.pbbaque.banking.transfer.exception.IdempotencyConflictException;
import com.pbbaque.banking.transfer.exception.TransferNotFoundException;
import com.pbbaque.banking.transfer.repository.TransferRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Application service responsible for coordinating banking transfers. */
@Service
public class TransferService {

  private final TransferRepository transferRepository;
  private final AccountClient accountClient;

  /**
   * Creates the transfer service.
   *
   * @param transferRepository transfer repository
   * @param accountClient client used to communicate with the account service
   */
  public TransferService(TransferRepository transferRepository, AccountClient accountClient) {
    this.transferRepository = transferRepository;
    this.accountClient = accountClient;
  }

  /**
   * Creates and processes a banking transfer.
   *
   * <p>If the supplied idempotency key was already processed, the existing transfer is returned
   * without executing the debit or credit again.
   *
   * @param request transfer request
   * @return processed transfer
   */
  public Transfer createTransfer(CreateTransferRequest request) {
    Transfer existingTransfer =
        transferRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);

    if (existingTransfer != null) {
      validateIdempotentRequest(existingTransfer, request);
      return existingTransfer;
    }

    Transfer transfer =
        Transfer.create(
            request.sourceAccountId(),
            request.destinationAccountId(),
            request.amount(),
            request.currency(),
            request.idempotencyKey());

    transfer = transferRepository.save(transfer);

    try {
      accountClient.debit(transfer.getSourceAccountId(), transfer.getAmount());
    } catch (AccountServiceException exception) {
      transfer.fail("Unable to debit source account");
      transferRepository.save(transfer);
      throw exception;
    }

    try {
      accountClient.credit(transfer.getDestinationAccountId(), transfer.getAmount());
    } catch (AccountServiceException creditException) {
      compensateSourceAccount(transfer, creditException);
      throw creditException;
    }

    transfer.complete();
    return transferRepository.save(transfer);
  }

  /**
   * Returns a transfer by its identifier.
   *
   * @param transferId transfer identifier
   * @return transfer when found
   */
  public Transfer getById(UUID transferId) {
    return transferRepository
        .findById(transferId)
        .orElseThrow(() -> new TransferNotFoundException(transferId));
  }

  /**
   * Returns a transfer by its idempotency key.
   *
   * @param idempotencyKey idempotency key
   * @return transfer when found
   */
  public Transfer getByIdempotencyKey(String idempotencyKey) {
    return transferRepository
        .findByIdempotencyKey(idempotencyKey)
        .orElseThrow(() -> new TransferNotFoundException(idempotencyKey));
  }

  private void compensateSourceAccount(
      Transfer transfer, AccountServiceException originalException) {

    try {
      accountClient.credit(transfer.getSourceAccountId(), transfer.getAmount());
      transfer.fail("Destination account could not be credited; source debit was compensated");
      transferRepository.save(transfer);
    } catch (AccountServiceException compensationException) {
      transfer.fail("Destination credit and source compensation both failed");
      transferRepository.save(transfer);

      compensationException.addSuppressed(originalException);
      throw compensationException;
    }
  }

  private void validateIdempotentRequest(Transfer existingTransfer, CreateTransferRequest request) {

    boolean sameRequest =
        existingTransfer.getSourceAccountId().equals(request.sourceAccountId())
            && existingTransfer.getDestinationAccountId().equals(request.destinationAccountId())
            && existingTransfer.getAmount().compareTo(request.amount()) == 0
            && existingTransfer.getCurrency().equalsIgnoreCase(request.currency().trim());

    if (!sameRequest) {
      throw new IdempotencyConflictException(request.idempotencyKey());
    }
  }
}
