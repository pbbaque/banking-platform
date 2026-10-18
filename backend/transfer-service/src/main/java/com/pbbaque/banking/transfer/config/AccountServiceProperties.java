package com.pbbaque.banking.transfer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties for communication with the account service. */
@ConfigurationProperties(prefix = "clients.account-service")
public record AccountServiceProperties(String baseUrl) {}
