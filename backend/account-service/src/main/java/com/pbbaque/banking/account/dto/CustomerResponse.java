package com.pbbaque.banking.account.dto;

import java.util.UUID;

/**
 * Customer information required by the account service.
 *
 * @param id customer identifier
 * @param identityUserId associated identity user identifier
 */
public record CustomerResponse(UUID id, UUID identityUserId) {}
