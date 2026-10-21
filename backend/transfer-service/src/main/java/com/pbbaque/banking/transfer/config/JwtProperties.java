package com.pbbaque.banking.transfer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Configuration properties used to validate JWT access tokens.
 *
 * @param publicKey RSA public key used to validate JWT signatures
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(Resource publicKey) {}
