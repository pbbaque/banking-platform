package com.pbbaque.banking.account.controller;

import com.pbbaque.banking.account.domain.Account;
import com.pbbaque.banking.account.dto.AccountResponse;
import com.pbbaque.banking.account.dto.AmountRequest;
import com.pbbaque.banking.account.dto.CreateAccountRequest;
import com.pbbaque.banking.account.service.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes REST operations for banking accounts. */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

  private final AccountService accountService;

  /**
   * Creates a new account controller.
   *
   * @param accountService account application service
   */
  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  /**
   * Creates a new banking account.
   *
   * @param request account creation payload
   * @return created account
   */
  @PostMapping
  public ResponseEntity<AccountResponse> createAccount(
      @Valid @RequestBody CreateAccountRequest request) {

    Account account =
        accountService.createAccount(
            request.customerId(), request.iban(), request.accountType(), request.currency());

    return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
  }

  /**
   * Finds an account by its identifier.
   *
   * @param id account identifier
   * @return matching account
   */
  @GetMapping("/{id}")
  public ResponseEntity<AccountResponse> getById(@PathVariable UUID id) {
    return ResponseEntity.ok(AccountResponse.from(accountService.getById(id)));
  }

  /**
   * Finds an account by its IBAN.
   *
   * @param iban account IBAN
   * @return matching account
   */
  @GetMapping("/iban/{iban}")
  public ResponseEntity<AccountResponse> getByIban(@PathVariable String iban) {
    return ResponseEntity.ok(AccountResponse.from(accountService.getByIban(iban)));
  }

  /**
   * Finds all accounts belonging to a customer.
   *
   * @param customerId customer identifier
   * @return customer accounts
   */
  @GetMapping
  public ResponseEntity<List<AccountResponse>> getByCustomerId(@RequestParam UUID customerId) {

    List<AccountResponse> accounts =
        accountService.getByCustomerId(customerId).stream().map(AccountResponse::from).toList();

    return ResponseEntity.ok(accounts);
  }

  /**
   * Blocks an account.
   *
   * @param id account identifier
   * @return updated account
   */
  @PutMapping("/{id}/block")
  public ResponseEntity<AccountResponse> block(@PathVariable UUID id) {
    return ResponseEntity.ok(AccountResponse.from(accountService.block(id)));
  }

  /**
   * Activates an account.
   *
   * @param id account identifier
   * @return updated account
   */
  @PutMapping("/{id}/activate")
  public ResponseEntity<AccountResponse> activate(@PathVariable UUID id) {
    return ResponseEntity.ok(AccountResponse.from(accountService.activate(id)));
  }

  /**
   * Closes an account.
   *
   * @param id account identifier
   * @return updated account
   */
  @PutMapping("/{id}/close")
  public ResponseEntity<AccountResponse> close(@PathVariable UUID id) {
    return ResponseEntity.ok(AccountResponse.from(accountService.close(id)));
  }

  /**
   * Credits funds to an account.
   *
   * @param id account identifier
   * @param operationId unique identifier used to make the operation idempotent
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
   * Debits funds from an account.
   *
   * @param id account identifier
   * @param operationId unique identifier used to make the operation idempotent
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
