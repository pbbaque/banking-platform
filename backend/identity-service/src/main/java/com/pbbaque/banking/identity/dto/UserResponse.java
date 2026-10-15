package com.pbbaque.banking.identity.dto;

import com.pbbaque.banking.identity.domain.User;
import com.pbbaque.banking.identity.domain.UserStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response representation of an identity user.
 *
 * @param id user identifier
 * @param username unique username
 * @param email user email address
 * @param status current user status
 * @param createdAt creation timestamp
 * @param updatedAt last update timestamp
 * @param lastLoginAt timestamp of the latest successful login
 */
public record UserResponse(
    UUID id,
    String username,
    String email,
    UserStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    OffsetDateTime lastLoginAt) {

  /**
   * Creates a response representation from a user entity.
   *
   * @param user user entity
   * @return user response
   */
  public static UserResponse from(User user) {
    return new UserResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getStatus(),
        user.getCreatedAt(),
        user.getUpdatedAt(),
        user.getLastLoginAt());
  }
}
