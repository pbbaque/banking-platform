package com.pbbaque.banking.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

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

    protected User() {
    }

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

    public static User create(
            String username,
            String email,
            String passwordHash) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        return new User(
                UUID.randomUUID(),
                username,
                email,
                passwordHash,
                UserStatus.PENDING,
                now,
                now,
                null);
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
        touch();
    }

    public void lock() {
        this.status = UserStatus.LOCKED;
        touch();
    }

    public void disable() {
        this.status = UserStatus.DISABLED;
        touch();
    }

    public void registerLogin() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        this.lastLoginAt = now;
        this.updatedAt = now;
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
}