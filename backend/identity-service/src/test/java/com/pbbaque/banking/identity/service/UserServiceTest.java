package com.pbbaque.banking.identity.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.pbbaque.banking.identity.domain.User;
import com.pbbaque.banking.identity.domain.UserStatus;
import com.pbbaque.banking.identity.dto.CreateUserRequest;
import com.pbbaque.banking.identity.dto.LoginRequest;
import com.pbbaque.banking.identity.dto.LoginResponse;
import com.pbbaque.banking.identity.dto.UserResponse;
import com.pbbaque.banking.identity.exception.InvalidCredentialsException;
import com.pbbaque.banking.identity.exception.InvalidUserStateTransitionException;
import com.pbbaque.banking.identity.exception.UserAlreadyExistsException;
import com.pbbaque.banking.identity.exception.UserNotFoundException;
import com.pbbaque.banking.identity.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private JwtService jwtService;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, passwordEncoder, jwtService);
  }

  @Test
  void shouldCreateUser() {
    CreateUserRequest request = createRequest();

    when(userRepository.existsByUsername(request.username())).thenReturn(false);

    when(userRepository.existsByEmail(request.email())).thenReturn(false);

    when(passwordEncoder.encode(request.password())).thenReturn("$2a$10$encodedPassword");

    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UserResponse response = userService.create(request);

    assertNotNull(response.id());
    assertEquals(request.username(), response.username());
    assertEquals(request.email(), response.email());
    assertEquals("PENDING", response.status().name());

    verify(passwordEncoder).encode(request.password());
    verify(userRepository).save(any(User.class));
  }

  @Test
  void shouldRejectDuplicateUsername() {
    CreateUserRequest request = createRequest();

    when(userRepository.existsByUsername(request.username())).thenReturn(true);

    assertThrows(UserAlreadyExistsException.class, () -> userService.create(request));

    verify(userRepository, never()).existsByEmail(anyString());
    verify(passwordEncoder, never()).encode(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void shouldRejectDuplicateEmail() {
    CreateUserRequest request = createRequest();

    when(userRepository.existsByUsername(request.username())).thenReturn(false);

    when(userRepository.existsByEmail(request.email())).thenReturn(true);

    assertThrows(UserAlreadyExistsException.class, () -> userService.create(request));

    verify(passwordEncoder, never()).encode(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void shouldGetUserById() {
    User user = createUser();

    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    UserResponse response = userService.getById(user.getId());

    assertEquals(user.getId(), response.id());
    assertEquals(user.getUsername(), response.username());
    assertEquals(user.getEmail(), response.email());
  }

  @Test
  void shouldThrowWhenUserByIdDoesNotExist() {
    UUID id = UUID.randomUUID();

    when(userRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> userService.getById(id));
  }

  @Test
  void shouldGetUserByUsername() {
    User user = createUser();

    when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

    UserResponse response = userService.getByUsername(user.getUsername());

    assertEquals(user.getId(), response.id());
    assertEquals(user.getUsername(), response.username());
    assertEquals(user.getEmail(), response.email());
  }

  @Test
  void shouldThrowWhenUsernameDoesNotExist() {
    String username = "99999999Z";

    when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> userService.getByUsername(username));
  }

  private CreateUserRequest createRequest() {
    return new CreateUserRequest("12345678A", "test@banking-platform.local", "SecurePassword123!");
  }

  private User createUser() {
    return User.create("12345678A", "test@banking-platform.local", "$2a$10$encodedPassword");
  }

  @Test
  void shouldActivateUser() {
    User user = createUser();

    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    UserResponse response = userService.activate(user.getId());

    assertEquals(UserStatus.ACTIVE, response.status());
  }

  @Test
  void shouldLockUser() {
    User user = createUser();
    user.activate();

    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    UserResponse response = userService.lock(user.getId());

    assertEquals(UserStatus.LOCKED, response.status());
  }

  @Test
  void shouldDisableUser() {
    User user = createUser();

    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    UserResponse response = userService.disable(user.getId());

    assertEquals(UserStatus.DISABLED, response.status());
  }

  @Test
  void shouldThrowWhenActivatingUnknownUser() {
    UUID id = UUID.randomUUID();

    when(userRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> userService.activate(id));
  }

  @Test
  void shouldPropagateInvalidStateTransition() {
    User user = createUser();

    user.disable();

    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    assertThrows(
        InvalidUserStateTransitionException.class, () -> userService.activate(user.getId()));
  }

  @Test
  void shouldAuthenticateActiveUser() {
    User user = createUser();

    user.activate();

    when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

    when(passwordEncoder.matches("SecurePassword123!", user.getPasswordHash())).thenReturn(true);
    when(jwtService.generateAccessToken(user)).thenReturn("test-access-token");

    LoginResponse response =
        userService.authenticate(new LoginRequest(user.getUsername(), "SecurePassword123!"));

    assertEquals(user.getId(), response.userId());
    assertEquals(user.getUsername(), response.username());
    assertEquals(UserStatus.ACTIVE, response.status());
    assertNotNull(response.lastLoginAt());
    assertEquals("test-access-token", response.accessToken());

    verify(jwtService).generateAccessToken(user);
  }

  @Test
  void shouldRejectAuthenticationWhenUsernameDoesNotExist() {
    when(userRepository.findByUsername("99999999Z")).thenReturn(Optional.empty());

    assertThrows(
        InvalidCredentialsException.class,
        () -> userService.authenticate(new LoginRequest("99999999Z", "SecurePassword123!")));
  }

  @Test
  void shouldRejectAuthenticationWhenPasswordIsInvalid() {
    User user = createUser();

    user.activate();

    when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

    when(passwordEncoder.matches("WrongPassword123!", user.getPasswordHash())).thenReturn(false);

    assertThrows(
        InvalidCredentialsException.class,
        () -> userService.authenticate(new LoginRequest(user.getUsername(), "WrongPassword123!")));

    verify(jwtService, never()).generateAccessToken(any(User.class));
  }

  @Test
  void shouldRejectAuthenticationWhenUserIsNotActive() {
    User user = createUser();

    when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

    when(passwordEncoder.matches("SecurePassword123!", user.getPasswordHash())).thenReturn(true);

    assertThrows(
        InvalidUserStateTransitionException.class,
        () -> userService.authenticate(new LoginRequest(user.getUsername(), "SecurePassword123!")));
  }
}
