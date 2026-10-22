package com.pbbaque.banking.transfer.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response received from account-service for a completed account balance operation.
 *
 * @param id operation identifier
 * @param accountId affected account identifier
 * @param operationType operation type
 * @param amount operation amount
 * @param resultingBalance account balance immediately after the operation
 * @param createdAt operation execution time
 */
public record AccountOperationResponse(
    UUID id,
    UUID accountId,
    String operationType,
    BigDecimal amount,
    BigDecimal resultingBalance,
    OffsetDateTime createdAt) {}
