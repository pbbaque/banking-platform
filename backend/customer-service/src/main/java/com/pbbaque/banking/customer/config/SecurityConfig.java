package com.pbbaque.banking.customer.config;

import com.pbbaque.banking.customer.security.InternalServiceAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Security configuration for customer-service. */
@Configuration
public class SecurityConfig {

  /**
   * Configures HTTP security for customer-service.
   *
   * @param http Spring Security configuration
   * @param internalServiceAuthenticationFilter internal service authentication filter
   * @return configured security filter chain
   * @throws Exception when security configuration fails
   */
  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, InternalServiceAuthenticationFilter internalServiceAuthenticationFilter)
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
                    .anyRequest()
                    .permitAll())
        .addFilterBefore(
            internalServiceAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
