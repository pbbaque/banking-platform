package com.pbbaque.banking.account.repository;

import com.pbbaque.banking.account.domain.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Provides persistence operations for banking accounts. */
public interface AccountRepository extends JpaRepository<Account, UUID> {

  /**
   * Finds an account by its IBAN.
   *
   * @param iban account IBAN
   * @return matching account when present
   */
  Optional<Account> findByIban(String iban);

  /**
   * Finds all accounts that belong to a customer.
   *
   * @param customerId customer identifier
   * @return accounts belonging to the customer
   */
  List<Account> findAllByCustomerId(UUID customerId);

  /**
   * Checks whether an account exists with the given IBAN.
   *
   * @param iban account IBAN
   * @return {@code true} when an account with the IBAN exists
   */
  boolean existsByIban(String iban);
}
