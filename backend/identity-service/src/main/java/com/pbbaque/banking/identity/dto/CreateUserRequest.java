package com.pbbaque.banking.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank @Size(max = 50) String username,

        @NotBlank @Email @Size(max = 254) String email,

        @NotBlank @Size(min = 12, max = 128) String password

) {
}