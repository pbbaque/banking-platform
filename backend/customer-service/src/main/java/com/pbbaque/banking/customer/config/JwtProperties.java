package com.pbbaque.banking.customer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Configuration properties for JWT validation.
 *
 * @param publicKey RSA public key used to validate access tokens
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(Resource publicKey) {}
