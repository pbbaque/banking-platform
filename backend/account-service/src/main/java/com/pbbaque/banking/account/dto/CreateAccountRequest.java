package com.pbbaque.banking.account.dto;

import com.pbbaque.banking.account.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Request payload used to create a banking account. */
public record CreateAccountRequest(
    @NotNull UUID customerId,
    @NotBlank @Size(max = 34) String iban,
    @NotNull AccountType accountType,
    @NotBlank @Size(min = 3, max = 3) String currency) {}
