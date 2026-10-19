package com.pbbaque.banking.transfer.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Represents account information returned by account-service. */
public record AccountResponse(
    UUID id,
    UUID customerId,
    String iban,
    String accountType,
    String currency,
    BigDecimal balance,
    String status,
    Long version,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    OffsetDateTime closedAt) {}
