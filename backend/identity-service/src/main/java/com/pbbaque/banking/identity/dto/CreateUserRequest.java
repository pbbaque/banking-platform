package com.pbbaque.banking.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new identity user.
 *
 * @param username unique username
 * @param email user email address
 * @param password raw password to be encoded before persistence
 */
public record CreateUserRequest(
    @NotBlank @Size(max = 50) String username,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 12, max = 128) String password) {}
