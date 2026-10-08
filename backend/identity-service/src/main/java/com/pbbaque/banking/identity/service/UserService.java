package com.pbbaque.banking.identity.service;

import com.pbbaque.banking.identity.domain.User;
import com.pbbaque.banking.identity.dto.CreateUserRequest;
import com.pbbaque.banking.identity.dto.UserResponse;
import com.pbbaque.banking.identity.exception.UserAlreadyExistsException;
import com.pbbaque.banking.identity.exception.UserNotFoundException;
import com.pbbaque.banking.identity.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException(
                    "A user already exists with username: "
                            + request.username());
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(
                    "A user already exists with email: "
                            + request.email());
        }

        String passwordHash = passwordEncoder.encode(request.password());

        User user = User.create(
                request.username(),
                request.email(),
                passwordHash);

        User savedUser = userRepository.save(user);

        return UserResponse.from(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return UserResponse.from(findById(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(
                        () -> new UserNotFoundException(
                                "User not found with username: "
                                        + username));

        return UserResponse.from(user);
    }

    private User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(
                        () -> new UserNotFoundException(
                                "User not found with id: " + id));
    }
}