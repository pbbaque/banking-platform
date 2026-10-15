package com.pbbaque.banking.identity.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.pbbaque.banking.identity.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

class JwtServiceTest {

  @Test
  void shouldGenerateAccessToken() {
    JwtEncoder jwtEncoder = mock(JwtEncoder.class);

    Jwt jwt = mock(Jwt.class);

    when(jwt.getTokenValue()).thenReturn("generated-access-token");

    when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

    JwtService jwtService = new JwtService(jwtEncoder);

    User user =
        User.create("12345678A", "test@banking-platform.local", "$2a$10$fakeHashForTestingOnly");

    String token = jwtService.generateAccessToken(user);

    assertEquals("generated-access-token", token);

    verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
  }
}
