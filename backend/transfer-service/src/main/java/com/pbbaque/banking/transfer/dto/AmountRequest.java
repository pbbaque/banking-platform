package com.pbbaque.banking.transfer.dto;

import java.math.BigDecimal;

/** Request payload sent to account-service for balance operations. */
public record AmountRequest(BigDecimal amount) {}
