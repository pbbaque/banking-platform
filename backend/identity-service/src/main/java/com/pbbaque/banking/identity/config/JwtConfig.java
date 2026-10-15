package com.pbbaque.banking.identity.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Configures JWT encoding and decoding using RSA keys. */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

  private final JwtProperties jwtProperties;

  public JwtConfig(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  /**
   * Creates the JWT encoder used to sign access tokens.
   *
   * @return configured JWT encoder
   * @throws Exception if the RSA keys cannot be loaded
   */
  @Bean
  public JwtEncoder jwtEncoder() throws Exception {
    RSAPublicKey publicKey = RsaKeyUtils.readPublicKey(jwtProperties.publicKey());

    RSAPrivateKey privateKey = RsaKeyUtils.readPrivateKey(jwtProperties.privateKey());

    RSAKey rsaKey = new RSAKey.Builder(publicKey).privateKey(privateKey).build();

    JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));

    return new NimbusJwtEncoder(jwkSource);
  }

  /**
   * Creates the JWT decoder used to validate access tokens.
   *
   * @return configured JWT decoder
   * @throws Exception if the RSA public key cannot be loaded
   */
  @Bean
  public JwtDecoder jwtDecoder() throws Exception {
    RSAPublicKey publicKey = RsaKeyUtils.readPublicKey(jwtProperties.publicKey());

    NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();

    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("banking-platform"));

    return decoder;
  }
}
