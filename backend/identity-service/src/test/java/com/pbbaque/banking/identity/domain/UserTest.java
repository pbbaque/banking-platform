package com.pbbaque.banking.identity.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.pbbaque.banking.identity.exception.InvalidUserStateTransitionException;
import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void shouldCreateUserAsPending() {
    User user =
        User.create("12345678A", "test@banking-platform.local", "$2a$10$fakeHashForTestingOnly");

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
  void shouldReactivateLockedUser() {
    User user = createUser();

    user.activate();
    user.lock();
    user.activate();

    assertEquals(UserStatus.ACTIVE, user.getStatus());
  }

  @Test
  void shouldRejectActivationFromActiveStatus() {
    User user = createUser();

    user.activate();

    assertThrows(InvalidUserStateTransitionException.class, user::activate);
  }

  @Test
  void shouldRejectActivationFromDisabledStatus() {
    User user = createUser();

    user.disable();

    assertThrows(InvalidUserStateTransitionException.class, user::activate);
  }

  @Test
  void shouldLockUser() {
    User user = createUser();

    user.activate();
    user.lock();

    assertEquals(UserStatus.LOCKED, user.getStatus());
    assertNotNull(user.getUpdatedAt());
  }

  @Test
  void shouldRejectLockFromPendingStatus() {
    User user = createUser();

    assertThrows(InvalidUserStateTransitionException.class, user::lock);
  }

  @Test
  void shouldRejectLockFromDisabledStatus() {
    User user = createUser();

    user.disable();

    assertThrows(InvalidUserStateTransitionException.class, user::lock);
  }

  @Test
  void shouldDisableUser() {
    User user = createUser();

    user.disable();

    assertEquals(UserStatus.DISABLED, user.getStatus());
    assertNotNull(user.getUpdatedAt());
  }

  @Test
  void shouldDisableActiveUser() {
    User user = createUser();

    user.activate();
    user.disable();

    assertEquals(UserStatus.DISABLED, user.getStatus());
  }

  @Test
  void shouldDisableLockedUser() {
    User user = createUser();

    user.activate();
    user.lock();
    user.disable();

    assertEquals(UserStatus.DISABLED, user.getStatus());
  }

  @Test
  void shouldRejectDisablingAlreadyDisabledUser() {
    User user = createUser();

    user.disable();

    assertThrows(InvalidUserStateTransitionException.class, user::disable);
  }

  @Test
  void shouldRegisterLogin() {
    User user = createUser();

    user.activate();
    user.registerLogin();

    assertNotNull(user.getLastLoginAt());
    assertNotNull(user.getUpdatedAt());
  }

  @Test
  void shouldRejectLoginWhenPending() {
    User user = createUser();

    assertThrows(InvalidUserStateTransitionException.class, user::registerLogin);
  }

  @Test
  void shouldRejectLoginWhenLocked() {
    User user = createUser();

    user.activate();
    user.lock();

    assertThrows(InvalidUserStateTransitionException.class, user::registerLogin);
  }

  @Test
  void shouldRejectLoginWhenDisabled() {
    User user = createUser();

    user.disable();

    assertThrows(InvalidUserStateTransitionException.class, user::registerLogin);
  }

  private User createUser() {
    return User.create("12345678A", "test@banking-platform.local", "$2a$10$fakeHashForTestingOnly");
  }
}
