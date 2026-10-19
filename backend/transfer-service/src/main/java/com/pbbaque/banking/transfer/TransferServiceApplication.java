package com.pbbaque.banking.transfer;

import com.pbbaque.banking.transfer.config.AccountServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Entry point for the transfer service. */
@EnableScheduling
@EnableConfigurationProperties(AccountServiceProperties.class)
@SpringBootApplication
public class TransferServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(TransferServiceApplication.class, args);
  }
}
