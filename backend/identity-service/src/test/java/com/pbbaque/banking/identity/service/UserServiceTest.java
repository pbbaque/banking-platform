package com.pbbaque.banking.identity.service;

import com.pbbaque.banking.identity.domain.User;
import com.pbbaque.banking.identity.dto.CreateUserRequest;
import com.pbbaque.banking.identity.dto.UserResponse;
import com.pbbaque.banking.identity.exception.UserAlreadyExistsException;
import com.pbbaque.banking.identity.exception.UserNotFoundException;
import com.pbbaque.banking.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                passwordEncoder);
    }

    @Test
    void shouldCreateUser() {
        CreateUserRequest request = createRequest();

        when(userRepository.existsByUsername(request.username()))
                .thenReturn(false);

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(false);

        when(passwordEncoder.encode(request.password()))
                .thenReturn("$2a$10$encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

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

        when(userRepository.existsByUsername(request.username()))
                .thenReturn(true);

        assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.create(request));

        verify(userRepository, never()).existsByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        CreateUserRequest request = createRequest();

        when(userRepository.existsByUsername(request.username()))
                .thenReturn(false);

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(true);

        assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.create(request));

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldGetUserById() {
        User user = createUser();

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        UserResponse response = userService.getById(user.getId());

        assertEquals(user.getId(), response.id());
        assertEquals(user.getUsername(), response.username());
        assertEquals(user.getEmail(), response.email());
    }

    @Test
    void shouldThrowWhenUserByIdDoesNotExist() {
        UUID id = UUID.randomUUID();

        when(userRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.getById(id));
    }

    @Test
    void shouldGetUserByUsername() {
        User user = createUser();

        when(userRepository.findByUsername(user.getUsername()))
                .thenReturn(Optional.of(user));

        UserResponse response = userService.getByUsername(user.getUsername());

        assertEquals(user.getId(), response.id());
        assertEquals(user.getUsername(), response.username());
        assertEquals(user.getEmail(), response.email());
    }

    @Test
    void shouldThrowWhenUsernameDoesNotExist() {
        String username = "99999999Z";

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.getByUsername(username));
    }

    private CreateUserRequest createRequest() {
        return new CreateUserRequest(
                "12345678A",
                "test@banking-platform.local",
                "SecurePassword123!");
    }

    private User createUser() {
        return User.create(
                "12345678A",
                "test@banking-platform.local",
                "$2a$10$encodedPassword");
    }
}