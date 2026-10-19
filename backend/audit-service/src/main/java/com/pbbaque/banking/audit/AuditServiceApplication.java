package com.pbbaque.banking.audit;

import com.pbbaque.banking.audit.config.AuditEventsTopicProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/** Entry point for the audit service. */
@EnableConfigurationProperties(AuditEventsTopicProperties.class)
@SpringBootApplication
public class AuditServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuditServiceApplication.class, args);
  }
}
