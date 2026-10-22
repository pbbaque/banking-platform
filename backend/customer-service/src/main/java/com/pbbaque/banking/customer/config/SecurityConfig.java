package com.pbbaque.banking.customer.config;

import com.pbbaque.banking.customer.security.InternalServiceAuthenticationFilter;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Security configuration for customer-service. */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

  private final JwtProperties jwtProperties;

  public SecurityConfig(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  /**
   * Creates the JWT decoder used to validate access tokens issued by identity-service.
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
   * Configures HTTP security for customer-service.
   *
   * @param http Spring Security configuration
   * @param internalServiceAuthenticationFilter internal service authentication filter
   * @param jwtAuthenticationConverter JWT authentication converter
   * @return configured security filter chain
   * @throws Exception when security configuration fails
   */
  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      InternalServiceAuthenticationFilter internalServiceAuthenticationFilter,
      JwtAuthenticationConverter jwtAuthenticationConverter)
      throws Exception {

    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorization ->
                authorization
                    .requestMatchers("/actuator/health")
                    .permitAll()
                    .requestMatchers("/api/internal/**")
                    .hasRole("INTERNAL_SERVICE")
                    .requestMatchers("/api/v1/customers/**")
                    .hasRole("USER")
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
        .addFilterBefore(
            internalServiceAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
