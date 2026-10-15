package com.pbbaque.banking.identity.service;

import com.pbbaque.banking.identity.domain.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Service responsible for generating JWT access tokens for authenticated users. */
@Service
public class JwtService {

  private final JwtEncoder jwtEncoder;

  public JwtService(JwtEncoder jwtEncoder) {
    this.jwtEncoder = jwtEncoder;
  }

  /**
   * Generates a signed JWT access token for the provided user.
   *
   * @param user authenticated user
   * @return signed JWT access token
   */
  public String generateAccessToken(User user) {
    Instant now = Instant.now();

    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer("banking-platform")
            .issuedAt(now)
            .expiresAt(now.plus(15, ChronoUnit.MINUTES))
            .subject(user.getId().toString())
            .claim("username", user.getUsername())
            .build();

    return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }
}
