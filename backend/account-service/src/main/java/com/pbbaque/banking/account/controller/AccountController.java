package com.pbbaque.banking.account.controller;

import com.pbbaque.banking.account.domain.Account;
import com.pbbaque.banking.account.dto.AccountResponse;
import com.pbbaque.banking.account.dto.CreateAccountRequest;
import com.pbbaque.banking.account.service.AccountAuthorizationService;
import com.pbbaque.banking.account.service.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes user-facing REST operations for banking accounts. */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

  private final AccountService accountService;
  private final AccountAuthorizationService accountAuthorizationService;

  /**
   * Creates a new account controller.
   *
   * @param accountService account application service
   * @param accountAuthorizationService account authorization service
   */
  public AccountController(
      AccountService accountService, AccountAuthorizationService accountAuthorizationService) {
    this.accountService = accountService;
    this.accountAuthorizationService = accountAuthorizationService;
  }

  /**
   * Creates a new banking account.
   *
   * @param request account creation payload
   * @param jwt authenticated access token
   * @return created account
   */
  @PostMapping
  public ResponseEntity<AccountResponse> createAccount(
      @Valid @RequestBody CreateAccountRequest request, @AuthenticationPrincipal Jwt jwt) {

    accountAuthorizationService.requireCustomerOwnership(request.customerId(), jwt);

    Account account =
        accountService.createAccount(
            request.customerId(), request.iban(), request.accountType(), request.currency());

    return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
  }

  /**
   * Finds an account by its identifier.
   *
   * @param id account identifier
   * @param jwt authenticated access token
   * @return matching account
   */
  @GetMapping("/{id}")
  public ResponseEntity<AccountResponse> getById(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {

    Account account = accountService.getById(id);

    accountAuthorizationService.requireOwnership(account, jwt);

    return ResponseEntity.ok(AccountResponse.from(account));
  }

  /**
   * Finds an account by its IBAN.
   *
   * @param iban account IBAN
   * @param jwt authenticated access token
   * @return matching account
   */
  @GetMapping("/iban/{iban}")
  public ResponseEntity<AccountResponse> getByIban(
      @PathVariable String iban, @AuthenticationPrincipal Jwt jwt) {

    Account account = accountService.getByIban(iban);

    accountAuthorizationService.requireOwnership(account, jwt);

    return ResponseEntity.ok(AccountResponse.from(account));
  }

  /**
   * Finds all accounts belonging to a customer.
   *
   * @param customerId customer identifier
   * @param jwt authenticated access token
   * @return customer accounts
   */
  @GetMapping
  public ResponseEntity<List<AccountResponse>> getByCustomerId(
      @RequestParam UUID customerId, @AuthenticationPrincipal Jwt jwt) {

    accountAuthorizationService.requireCustomerOwnership(customerId, jwt);

    List<AccountResponse> accounts =
        accountService.getByCustomerId(customerId).stream().map(AccountResponse::from).toList();

    return ResponseEntity.ok(accounts);
  }

  /**
   * Blocks an account.
   *
   * @param id account identifier
   * @param jwt authenticated access token
   * @return updated account
   */
  @PutMapping("/{id}/block")
  public ResponseEntity<AccountResponse> block(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {

    Account account = accountService.getById(id);

    accountAuthorizationService.requireOwnership(account, jwt);

    return ResponseEntity.ok(AccountResponse.from(accountService.block(id)));
  }

  /**
   * Activates an account.
   *
   * @param id account identifier
   * @param jwt authenticated access token
   * @return updated account
   */
  @PutMapping("/{id}/activate")
  public ResponseEntity<AccountResponse> activate(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {

    Account account = accountService.getById(id);

    accountAuthorizationService.requireOwnership(account, jwt);

    return ResponseEntity.ok(AccountResponse.from(accountService.activate(id)));
  }

  /**
   * Closes an account.
   *
   * @param id account identifier
   * @param jwt authenticated access token
   * @return updated account
   */
  @PutMapping("/{id}/close")
  public ResponseEntity<AccountResponse> close(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {

    Account account = accountService.getById(id);

    accountAuthorizationService.requireOwnership(account, jwt);

    return ResponseEntity.ok(AccountResponse.from(accountService.close(id)));
  }
}
