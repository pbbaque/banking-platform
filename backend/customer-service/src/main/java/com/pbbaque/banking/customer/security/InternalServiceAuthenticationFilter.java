package com.pbbaque.banking.customer.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Authenticates requests made by trusted internal services. */
@Component
public class InternalServiceAuthenticationFilter extends OncePerRequestFilter {

  private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

  private final byte[] configuredToken;

  /**
   * Creates the internal service authentication filter.
   *
   * @param internalToken token shared between internal services
   */
  public InternalServiceAuthenticationFilter(
      @Value("${security.internal.token}") String internalToken) {
    this.configuredToken = internalToken.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/internal/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String receivedToken = request.getHeader(INTERNAL_TOKEN_HEADER);

    if (receivedToken == null
        || !MessageDigest.isEqual(
            configuredToken, receivedToken.getBytes(StandardCharsets.UTF_8))) {

      response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid internal service token");
      return;
    }

    var authentication =
        new UsernamePasswordAuthenticationToken(
            "internal-service", null, List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE")));

    SecurityContextHolder.getContext().setAuthentication(authentication);

    filterChain.doFilter(request, response);
  }
}
