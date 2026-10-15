package com.pbbaque.banking.identity.service;

import com.pbbaque.banking.identity.domain.User;
import com.pbbaque.banking.identity.dto.CreateUserRequest;
import com.pbbaque.banking.identity.dto.LoginRequest;
import com.pbbaque.banking.identity.dto.LoginResponse;
import com.pbbaque.banking.identity.dto.UserResponse;
import com.pbbaque.banking.identity.exception.InvalidCredentialsException;
import com.pbbaque.banking.identity.exception.UserAlreadyExistsException;
import com.pbbaque.banking.identity.exception.UserNotFoundException;
import com.pbbaque.banking.identity.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service responsible for identity user operations and authentication. */
@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  /**
   * Creates the user service with its required dependencies.
   *
   * @param userRepository repository used to access users
   * @param passwordEncoder encoder used to hash and verify passwords
   * @param jwtService service used to generate access tokens
   */
  public UserService(
      UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  /**
   * Creates a new identity user.
   *
   * @param request user creation request
   * @return created user
   */
  @Transactional
  public UserResponse create(CreateUserRequest request) {
    if (userRepository.existsByUsername(request.username())) {
      throw new UserAlreadyExistsException(
          "A user already exists with username: " + request.username());
    }

    if (userRepository.existsByEmail(request.email())) {
      throw new UserAlreadyExistsException("A user already exists with email: " + request.email());
    }

    String passwordHash = passwordEncoder.encode(request.password());

    User user = User.create(request.username(), request.email(), passwordHash);

    User savedUser = userRepository.save(user);

    return UserResponse.from(savedUser);
  }

  /**
   * Retrieves a user by identifier.
   *
   * @param id user identifier
   * @return matching user
   */
  @Transactional(readOnly = true)
  public UserResponse getById(UUID id) {
    return UserResponse.from(findById(id));
  }

  /**
   * Retrieves a user by username.
   *
   * @param username username to search for
   * @return matching user
   */
  @Transactional(readOnly = true)
  public UserResponse getByUsername(String username) {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(
                () -> new UserNotFoundException("User not found with username: " + username));

    return UserResponse.from(user);
  }

  private User findById(UUID id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
  }

  /**
   * Activates a pending or locked user.
   *
   * @param id user identifier
   * @return updated user
   */
  @Transactional
  public UserResponse activate(UUID id) {
    User user = findById(id);

    user.activate();

    return UserResponse.from(user);
  }

  /**
   * Locks an active user.
   *
   * @param id user identifier
   * @return updated user
   */
  @Transactional
  public UserResponse lock(UUID id) {
    User user = findById(id);

    user.lock();

    return UserResponse.from(user);
  }

  /**
   * Disables a user.
   *
   * @param id user identifier
   * @return updated user
   */
  @Transactional
  public UserResponse disable(UUID id) {
    User user = findById(id);

    user.disable();

    return UserResponse.from(user);
  }

  /**
   * Authenticates a user and generates an access token.
   *
   * @param request login request
   * @return authentication response containing the access token
   */
  @Transactional
  public LoginResponse authenticate(LoginRequest request) {
    User user =
        userRepository
            .findByUsername(request.username())
            .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new InvalidCredentialsException("Invalid username or password");
    }

    user.registerLogin();

    String accessToken = jwtService.generateAccessToken(user);

    return new LoginResponse(
        user.getId(), user.getUsername(), user.getStatus(), user.getLastLoginAt(), accessToken);
  }
}
