package com.pbbaque.banking.identity.repository;

import com.pbbaque.banking.identity.domain.User;
import com.pbbaque.banking.identity.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("local")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserById() {
        User user = createUser(
                "11111111A",
                "repository1@banking-platform.local");

        User savedUser = userRepository.saveAndFlush(user);

        Optional<User> result = userRepository.findById(savedUser.getId());

        assertTrue(result.isPresent());
        assertEquals(
                savedUser.getId(),
                result.get().getId());
        assertEquals(
                "11111111A",
                result.get().getUsername());
    }

    @Test
    void shouldFindUserByUsername() {
        User user = createUser(
                "22222222B",
                "repository2@banking-platform.local");

        User savedUser = userRepository.saveAndFlush(user);

        Optional<User> result = userRepository.findByUsername(
                savedUser.getUsername());

        assertTrue(result.isPresent());
        assertEquals(
                savedUser.getId(),
                result.get().getId());
    }

    @Test
    void shouldFindUserByEmail() {
        User user = createUser(
                "33333333C",
                "repository3@banking-platform.local");

        User savedUser = userRepository.saveAndFlush(user);

        Optional<User> result = userRepository.findByEmail(
                savedUser.getEmail());

        assertTrue(result.isPresent());
        assertEquals(
                savedUser.getId(),
                result.get().getId());
    }

    @Test
    void shouldDetectExistingUsername() {
        User user = createUser(
                "44444444D",
                "repository4@banking-platform.local");

        User savedUser = userRepository.saveAndFlush(user);

        boolean exists = userRepository.existsByUsername(
                savedUser.getUsername());

        assertTrue(exists);
    }

    @Test
    void shouldDetectExistingEmail() {
        User user = createUser(
                "55555555E",
                "repository5@banking-platform.local");

        User savedUser = userRepository.saveAndFlush(user);

        boolean exists = userRepository.existsByEmail(
                savedUser.getEmail());

        assertTrue(exists);
    }

    @Test
    void shouldPersistUserStatusChanges() {
        User user = createUser(
                "66666666F",
                "repository6@banking-platform.local");

        User savedUser = userRepository.saveAndFlush(user);

        savedUser.activate();

        userRepository.flush();

        Optional<User> result = userRepository.findById(savedUser.getId());

        assertTrue(result.isPresent());
        assertEquals(
                UserStatus.ACTIVE,
                result.get().getStatus());
    }

    @Test
    void shouldPersistLastLoginAt() {
        User user = createUser(
                "77777777G",
                "repository7@banking-platform.local");

        User savedUser = userRepository.saveAndFlush(user);

        savedUser.registerLogin();

        userRepository.flush();

        Optional<User> result = userRepository.findById(savedUser.getId());

        assertTrue(result.isPresent());
        assertNotNull(
                result.get().getLastLoginAt());
    }

    private User createUser(
            String username,
            String email) {
        return User.create(
                username,
                email,
                "$2a$10$integrationTestHash");
    }
}