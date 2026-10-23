package com.pbbaque.banking.transfer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request used to create a banking transfer.
 *
 * @param sourceAccountId source account identifier
 * @param destinationIban destination account IBAN
 * @param amount amount to transfer
 * @param currency transfer currency
 * @param idempotencyKey unique key used to prevent duplicate processing
 */
public record CreateTransferRequest(
    @NotNull UUID sourceAccountId,
    @NotBlank @Size(max = 34) String destinationIban,
    @NotNull @DecimalMin(value = "0.0001") @Digits(integer = 15, fraction = 4) BigDecimal amount,
    @NotBlank @Size(max = 3) String currency,
    @NotBlank @Size(max = 100) String idempotencyKey) {}
