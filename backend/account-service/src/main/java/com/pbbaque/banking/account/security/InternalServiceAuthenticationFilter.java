package com.pbbaque.banking.account.security;

import com.pbbaque.banking.account.config.InternalServiceProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Authenticates trusted service-to-service requests using a shared internal token. */
public class InternalServiceAuthenticationFilter extends OncePerRequestFilter {

  public static final String INTERNAL_SERVICE_HEADER = "X-Internal-Service-Token";

  private static final String INTERNAL_SERVICE_AUTHORITY = "ROLE_INTERNAL_SERVICE";

  private final InternalServiceProperties properties;

  /**
   * Creates the internal service authentication filter.
   *
   * @param properties internal service authentication configuration
   */
  public InternalServiceAuthenticationFilter(InternalServiceProperties properties) {
    this.properties = properties;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String suppliedToken = request.getHeader(INTERNAL_SERVICE_HEADER);

    if (isValidToken(suppliedToken)) {
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              "internal-service",
              null,
              List.of(new SimpleGrantedAuthority(INTERNAL_SERVICE_AUTHORITY)));

      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }

  private boolean isValidToken(String suppliedToken) {
    String configuredToken = properties.token();

    if (suppliedToken == null || configuredToken == null) {
      return false;
    }

    return MessageDigest.isEqual(
        suppliedToken.getBytes(StandardCharsets.UTF_8),
        configuredToken.getBytes(StandardCharsets.UTF_8));
  }
}
