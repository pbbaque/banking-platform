package com.pbbaque.banking.audit.dto;

import com.pbbaque.banking.audit.domain.AuditOutcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Request used to create a new audit event. */
public record CreateAuditEventRequest(
    @NotBlank @Size(max = 100) String eventType,
    @NotBlank @Size(max = 100) String aggregateType,
    @NotBlank @Size(max = 100) String aggregateId,
    UUID actorId,
    @NotNull AuditOutcome outcome,
    @Size(max = 500) String description,
    String metadata,
    @NotNull OffsetDateTime occurredAt) {}
