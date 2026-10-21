package com.pbbaque.banking.account.config;

import com.pbbaque.banking.account.security.InternalServiceAuthenticationFilter;
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

/** Configures authentication and authorization for the account service. */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, InternalServiceProperties.class})
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
   * Creates the authentication filter used by trusted internal services.
   *
   * @param properties internal service security configuration
   * @return internal authentication filter
   */
  @Bean
  public InternalServiceAuthenticationFilter internalServiceAuthenticationFilter(
      InternalServiceProperties properties) {
    return new InternalServiceAuthenticationFilter(properties);
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
   * Configures stateless HTTP security.
   *
   * @param http HTTP security configuration
   * @param internalServiceAuthenticationFilter internal service authentication filter
   * @param jwtAuthenticationConverter JWT authentication converter
   * @return configured security filter chain
   * @throws Exception when the filter chain cannot be built
   */
  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      InternalServiceAuthenticationFilter internalServiceAuthenticationFilter,
      JwtAuthenticationConverter jwtAuthenticationConverter)
      throws Exception {

    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health")
                    .permitAll()
                    .requestMatchers("/api/internal/accounts/**")
                    .hasRole("INTERNAL_SERVICE")
                    .requestMatchers("/api/accounts/**")
                    .hasRole("USER")
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
        .addFilterBefore(
            internalServiceAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
