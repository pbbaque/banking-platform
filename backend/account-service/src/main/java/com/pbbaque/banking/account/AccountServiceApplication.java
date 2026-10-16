package com.pbbaque.banking.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the account service application. */
@SpringBootApplication
public class AccountServiceApplication {

  /**
   * Starts the account service.
   *
   * @param args application arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(AccountServiceApplication.class, args);
  }
}
