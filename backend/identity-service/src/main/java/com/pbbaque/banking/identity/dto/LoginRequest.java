package com.pbbaque.banking.identity.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for authenticating an identity user.
 *
 * @param username username used for authentication
 * @param password raw password provided by the user
 */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
