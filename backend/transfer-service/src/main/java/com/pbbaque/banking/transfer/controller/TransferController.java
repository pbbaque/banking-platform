package com.pbbaque.banking.transfer.controller;

import com.pbbaque.banking.transfer.domain.Transfer;
import com.pbbaque.banking.transfer.dto.CreateTransferRequest;
import com.pbbaque.banking.transfer.dto.TransferResponse;
import com.pbbaque.banking.transfer.service.TransferService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for banking transfers. */
@RestController
@RequestMapping("/api/transfers")
public class TransferController {

  private final TransferService transferService;

  /**
   * Creates the transfer controller.
   *
   * @param transferService transfer application service
   */
  public TransferController(TransferService transferService) {
    this.transferService = transferService;
  }

  /**
   * Creates and processes a banking transfer.
   *
   * @param request transfer creation request
   * @return created transfer
   */
  @PostMapping
  public ResponseEntity<TransferResponse> createTransfer(
      @Valid @RequestBody CreateTransferRequest request) {

    Transfer transfer = transferService.createTransfer(request);

    return ResponseEntity.status(HttpStatus.CREATED).body(TransferResponse.from(transfer));
  }

  /**
   * Returns a transfer by its identifier.
   *
   * @param id transfer identifier
   * @return transfer response
   */
  @GetMapping("/{id}")
  public ResponseEntity<TransferResponse> getById(@PathVariable UUID id) {
    Transfer transfer = transferService.getById(id);

    return ResponseEntity.ok(TransferResponse.from(transfer));
  }

  /**
   * Returns a transfer by its idempotency key.
   *
   * @param idempotencyKey transfer idempotency key
   * @return transfer response
   */
  @GetMapping
  public ResponseEntity<TransferResponse> getByIdempotencyKey(@RequestParam String idempotencyKey) {

    Transfer transfer = transferService.getByIdempotencyKey(idempotencyKey);

    return ResponseEntity.ok(TransferResponse.from(transfer));
  }
}
