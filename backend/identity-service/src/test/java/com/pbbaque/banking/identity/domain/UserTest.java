package com.pbbaque.banking.identity.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldCreateUserAsPending() {
        User user = User.create(
                "12345678A",
                "test@banking-platform.local",
                "$2a$10$fakeHashForTestingOnly");

        assertNotNull(user.getId());
        assertEquals("12345678A", user.getUsername());
        assertEquals("test@banking-platform.local", user.getEmail());
        assertEquals("$2a$10$fakeHashForTestingOnly", user.getPasswordHash());
        assertEquals(UserStatus.PENDING, user.getStatus());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertNull(user.getLastLoginAt());
    }

    @Test
    void shouldActivateUser() {
        User user = createUser();

        user.activate();

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void shouldLockUser() {
        User user = createUser();

        user.lock();

        assertEquals(UserStatus.LOCKED, user.getStatus());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void shouldDisableUser() {
        User user = createUser();

        user.disable();

        assertEquals(UserStatus.DISABLED, user.getStatus());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void shouldRegisterLogin() {
        User user = createUser();

        user.registerLogin();

        assertNotNull(user.getLastLoginAt());
        assertNotNull(user.getUpdatedAt());
    }

    private User createUser() {
        return User.create(
                "12345678A",
                "test@banking-platform.local",
                "$2a$10$fakeHashForTestingOnly");
    }
}