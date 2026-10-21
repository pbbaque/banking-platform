package com.pbbaque.banking.transfer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for communication with account-service.
 *
 * @param baseUrl account-service base URL
 * @param internalToken authentication token used for trusted internal requests
 */
@ConfigurationProperties(prefix = "clients.account-service")
public record AccountServiceProperties(String baseUrl, String internalToken) {}
