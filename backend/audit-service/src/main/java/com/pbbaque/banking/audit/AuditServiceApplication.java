package com.pbbaque.banking.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the audit service. */
@SpringBootApplication
public class AuditServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuditServiceApplication.class, args);
  }
}
