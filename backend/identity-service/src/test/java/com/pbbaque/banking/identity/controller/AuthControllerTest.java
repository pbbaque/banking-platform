package com.pbbaque.banking.identity.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbbaque.banking.identity.config.SecurityConfig;
import com.pbbaque.banking.identity.domain.UserStatus;
import com.pbbaque.banking.identity.dto.LoginRequest;
import com.pbbaque.banking.identity.dto.LoginResponse;
import com.pbbaque.banking.identity.exception.GlobalExceptionHandler;
import com.pbbaque.banking.identity.exception.InvalidCredentialsException;
import com.pbbaque.banking.identity.exception.InvalidUserStateTransitionException;
import com.pbbaque.banking.identity.service.UserService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private UserService userService;

  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void shouldLoginSuccessfully() throws Exception {
    UUID userId = UUID.randomUUID();
    OffsetDateTime lastLoginAt = OffsetDateTime.now(ZoneOffset.UTC);

    LoginRequest request = new LoginRequest("12345678A", "SecurePassword123!");

    LoginResponse response =
        new LoginResponse(userId, "12345678A", UserStatus.ACTIVE, lastLoginAt, "test-access-token");

    when(userService.authenticate(any(LoginRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId.toString()))
        .andExpect(jsonPath("$.username").value("12345678A"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.lastLoginAt").exists())
        .andExpect(jsonPath("$.accessToken").value("test-access-token"));
  }

  @Test
  void shouldReturn400WhenLoginRequestIsInvalid() throws Exception {
    String request =
        """
                                {
                                    "username": "",
                                    "password": ""
                                }
                                """;

    mockMvc
        .perform(
            post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"));
  }

  @Test
  void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
    LoginRequest request = new LoginRequest("12345678A", "WrongPassword123!");

    when(userService.authenticate(any(LoginRequest.class)))
        .thenThrow(new InvalidCredentialsException("Invalid username or password"));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.error").value("Unauthorized"))
        .andExpect(jsonPath("$.message").value("Invalid username or password"));
  }

  @Test
  void shouldReturn409WhenUserCannotLoginBecauseOfStatus() throws Exception {

    LoginRequest request = new LoginRequest("12345678A", "SecurePassword123!");

    when(userService.authenticate(any(LoginRequest.class)))
        .thenThrow(
            new InvalidUserStateTransitionException(
                "Cannot register login for user with status: LOCKED"));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.error").value("Conflict"))
        .andExpect(
            jsonPath("$.message").value("Cannot register login for user with status: LOCKED"));
  }
}
