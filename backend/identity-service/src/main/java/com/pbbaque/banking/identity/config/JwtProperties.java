package com.pbbaque.banking.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Configuration properties for the RSA keys used to sign and validate JWTs.
 *
 * @param publicKey public RSA key used to validate JWT signatures
 * @param privateKey private RSA key used to sign JWTs
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(Resource publicKey, Resource privateKey) {}
