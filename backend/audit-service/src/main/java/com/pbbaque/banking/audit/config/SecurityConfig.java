package com.pbbaque.banking.audit.config;

import java.security.interfaces.RSAPublicKey;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/** Configures JWT authentication and authorization for audit-service. */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

  private final JwtProperties jwtProperties;

  /**
   * Creates the audit service security configuration.
   *
   * @param jwtProperties JWT validation configuration
   */
  public SecurityConfig(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  /**
   * Creates the JWT decoder used to validate tokens issued by identity-service.
   *
   * @return configured JWT decoder
   * @throws Exception when the RSA public key cannot be loaded
   */
  @Bean
  public JwtDecoder jwtDecoder() throws Exception {
    RSAPublicKey publicKey = RsaKeyUtils.readPublicKey(jwtProperties.publicKey());

    NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();

    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("banking-platform"));

    return decoder;
  }

  /**
   * Converts valid identity JWTs into authenticated banking users.
   *
   * @return JWT authentication converter
   */
  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> List.of(new SimpleGrantedAuthority("ROLE_USER")));

    return converter;
  }

  /**
   * Configures stateless HTTP security for audit-service.
   *
   * <p>Audit events may only be created through trusted asynchronous messaging. HTTP clients are
   * allowed to query audit data only when authenticated as a banking user.
   *
   * @param http HTTP security configuration
   * @param jwtAuthenticationConverter JWT authentication converter
   * @return configured security filter chain
   * @throws Exception when the filter chain cannot be built
   */
  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {

    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorization ->
                authorization
                    .requestMatchers("/actuator/health")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/audit-events/**")
                    .denyAll()
                    .requestMatchers("/api/audit-events/**")
                    .hasRole("USER")
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

    return http.build();
  }
}
