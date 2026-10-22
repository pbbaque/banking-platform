package com.pbbaque.banking.transfer.controller;

import com.pbbaque.banking.transfer.domain.Transfer;
import com.pbbaque.banking.transfer.dto.CreateTransferRequest;
import com.pbbaque.banking.transfer.dto.TransferResponse;
import com.pbbaque.banking.transfer.service.TransferService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
   * @param jwt authenticated user's JWT
   * @return created transfer
   */
  @PostMapping
  public ResponseEntity<TransferResponse> createTransfer(
      @Valid @RequestBody CreateTransferRequest request, @AuthenticationPrincipal Jwt jwt) {

    Transfer transfer = transferService.createTransfer(request, jwt.getTokenValue());

    BigDecimal balanceAfter =
        transferService.getBalanceAfter(transfer, transfer.getSourceAccountId());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(TransferResponse.from(transfer, balanceAfter));
  }

  /**
   * Returns a transfer by its identifier.
   *
   * @param id transfer identifier
   * @param jwt authenticated user's JWT
   * @return transfer response
   */
  @GetMapping("/{id}")
  public ResponseEntity<TransferResponse> getById(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {

    Transfer transfer = transferService.getById(id, jwt.getTokenValue());

    BigDecimal balanceAfter = transferService.getBalanceAfterForUser(transfer, jwt.getTokenValue());

    return ResponseEntity.ok(TransferResponse.from(transfer, balanceAfter));
  }

  /**
   * Returns a transfer by its idempotency key.
   *
   * @param idempotencyKey transfer idempotency key
   * @param jwt authenticated user's JWT
   * @return transfer response
   */
  @GetMapping(params = "idempotencyKey")
  public ResponseEntity<TransferResponse> getByIdempotencyKey(
      @RequestParam String idempotencyKey, @AuthenticationPrincipal Jwt jwt) {

    Transfer transfer = transferService.getByIdempotencyKey(idempotencyKey, jwt.getTokenValue());

    BigDecimal balanceAfter =
        transferService.getBalanceAfter(transfer, transfer.getSourceAccountId());

    return ResponseEntity.ok(TransferResponse.from(transfer, balanceAfter));
  }

  /**
   * Returns transfers involving one owned account.
   *
   * @param accountId account identifier
   * @param jwt authenticated user's JWT
   * @return account transfer history
   */
  @GetMapping(params = "accountId")
  public ResponseEntity<List<TransferResponse>> getByAccountId(
      @RequestParam UUID accountId, @AuthenticationPrincipal Jwt jwt) {

    List<TransferResponse> response =
        transferService.getByAccountId(accountId, jwt.getTokenValue()).stream()
            .map(
                transfer ->
                    TransferResponse.from(
                        transfer, transferService.getBalanceAfter(transfer, accountId)))
            .toList();

    return ResponseEntity.ok(response);
  }
}
