package com.pbbaque.banking.account.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties used to authenticate trusted internal service requests.
 *
 * @param token shared token used by trusted internal services
 */
@ConfigurationProperties(prefix = "security.internal")
public record InternalServiceProperties(String token) {}
