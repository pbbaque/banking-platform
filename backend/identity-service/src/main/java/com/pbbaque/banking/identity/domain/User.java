package com.pbbaque.banking.identity.domain;

import com.pbbaque.banking.identity.exception.InvalidUserStateTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Represents a digital identity user in the banking platform. */
@Entity
@Table(name = "users")
public class User {

  @Id
  @JdbcTypeCode(SqlTypes.BINARY)
  @Column(name = "id", nullable = false, columnDefinition = "RAW(16)")
  private UUID id;

  @Column(name = "username", nullable = false, length = 50, unique = true)
  private String username;

  @Column(name = "email", nullable = false, length = 254, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private UserStatus status;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @Column(name = "last_login_at")
  private OffsetDateTime lastLoginAt;

  protected User() {}

  private User(
      UUID id,
      String username,
      String email,
      String passwordHash,
      UserStatus status,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt,
      OffsetDateTime lastLoginAt) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.passwordHash = passwordHash;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.lastLoginAt = lastLoginAt;
  }

  /**
   * Creates a new user in pending status.
   *
   * @param username unique username
   * @param email unique email address
   * @param passwordHash encoded password
   * @return new user instance
   */
  public static User create(String username, String email, String passwordHash) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    return new User(
        UUID.randomUUID(), username, email, passwordHash, UserStatus.PENDING, now, now, null);
  }

  /** Registers a successful login for an active user. */
  public void registerLogin() {
    if (status != UserStatus.ACTIVE) {
      throw new InvalidUserStateTransitionException(
          "Cannot register login for user with status: " + status);
    }

    lastLoginAt = OffsetDateTime.now(ZoneOffset.UTC);
    touch();
  }

  private void touch() {
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public UUID getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public UserStatus getStatus() {
    return status;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public OffsetDateTime getLastLoginAt() {
    return lastLoginAt;
  }

  /** Activates a pending or locked user. */
  public void activate() {
    if (status != UserStatus.PENDING && status != UserStatus.LOCKED) {
      throw new InvalidUserStateTransitionException("Cannot activate user from status: " + status);
    }

    status = UserStatus.ACTIVE;
    touch();
  }

  /** Locks an active user. */
  public void lock() {
    if (status != UserStatus.ACTIVE) {
      throw new InvalidUserStateTransitionException("Cannot lock user from status: " + status);
    }

    status = UserStatus.LOCKED;
    touch();
  }

  /** Disables a user. */
  public void disable() {
    if (status == UserStatus.DISABLED) {
      throw new InvalidUserStateTransitionException("User is already disabled");
    }

    status = UserStatus.DISABLED;
    touch();
  }
}
