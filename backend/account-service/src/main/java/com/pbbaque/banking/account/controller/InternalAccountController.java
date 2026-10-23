package com.pbbaque.banking.account.controller;

import com.pbbaque.banking.account.dto.AccountOperationResponse;
import com.pbbaque.banking.account.dto.AccountResponse;
import com.pbbaque.banking.account.dto.AmountRequest;
import com.pbbaque.banking.account.service.AccountService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes account operations intended exclusively for trusted internal services. */
@RestController
@RequestMapping("/api/internal/accounts")
public class InternalAccountController {

  private final AccountService accountService;

  /**
   * Creates the internal account controller.
   *
   * @param accountService account application service
   */
  public InternalAccountController(AccountService accountService) {

    this.accountService = accountService;
  }

  /**
   * Retrieves an account for an internal service.
   *
   * @param id account identifier
   * @return matching account
   */
  @GetMapping("/{id}")
  public ResponseEntity<AccountResponse> getById(@PathVariable UUID id) {

    return ResponseEntity.ok(AccountResponse.from(accountService.getById(id)));
  }

  /**
   * Retrieves an account by IBAN for an internal service.
   *
   * @param iban account IBAN
   * @return matching account
   */
  @GetMapping("/iban/{iban}")
  public ResponseEntity<AccountResponse> getByIban(@PathVariable String iban) {

    return ResponseEntity.ok(AccountResponse.from(accountService.getByIban(iban)));
  }

  /**
   * Retrieves a completed account operation.
   *
   * @param operationId operation identifier
   * @return matching account operation
   */
  @GetMapping("/operations/{operationId}")
  public ResponseEntity<AccountOperationResponse> getOperationById(@PathVariable UUID operationId) {

    return ResponseEntity.ok(
        AccountOperationResponse.from(accountService.getOperationById(operationId)));
  }

  /**
   * Credits funds to an account idempotently.
   *
   * @param id account identifier
   * @param operationId logical operation identifier
   * @param request amount payload
   * @return updated account
   */
  @PostMapping("/{id}/credit")
  public ResponseEntity<AccountResponse> credit(
      @PathVariable UUID id,
      @RequestHeader("Idempotency-Key") UUID operationId,
      @Valid @RequestBody AmountRequest request) {

    return ResponseEntity.ok(
        AccountResponse.from(accountService.credit(id, request.amount(), operationId)));
  }

  /**
   * Debits funds from an account idempotently.
   *
   * @param id account identifier
   * @param operationId logical operation identifier
   * @param request amount payload
   * @return updated account
   */
  @PostMapping("/{id}/debit")
  public ResponseEntity<AccountResponse> debit(
      @PathVariable UUID id,
      @RequestHeader("Idempotency-Key") UUID operationId,
      @Valid @RequestBody AmountRequest request) {

    return ResponseEntity.ok(
        AccountResponse.from(accountService.debit(id, request.amount(), operationId)));
  }
}
