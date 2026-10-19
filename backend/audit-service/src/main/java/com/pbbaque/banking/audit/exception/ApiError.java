package com.pbbaque.banking.audit.exception;

import java.time.OffsetDateTime;

/** Standard API error response returned by the audit service. */
public record ApiError(
    OffsetDateTime timestamp, int status, String error, String message, String path) {}
