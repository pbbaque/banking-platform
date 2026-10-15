package com.pbbaque.banking.identity.dto;

import com.pbbaque.banking.identity.domain.UserStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response returned after a successful user authentication.
 *
 * @param userId authenticated user identifier
 * @param username authenticated username
 * @param status current user status
 * @param lastLoginAt timestamp of the latest successful login
 * @param accessToken JWT access token issued for the authenticated user
 */
public record LoginResponse(
    UUID userId,
    String username,
    UserStatus status,
    OffsetDateTime lastLoginAt,
    String accessToken) {}
