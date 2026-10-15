package com.pbbaque.banking.customer.dto;

import com.pbbaque.banking.customer.domain.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request payload for creating a new banking customer.
 *
 * @param identityUserId identifier of the associated identity user
 * @param firstName customer first name
 * @param lastName customer last name
 * @param documentType customer identification document type
 * @param documentNumber customer identification document number
 * @param dateOfBirth customer date of birth
 * @param phoneNumber customer phone number
 * @param fiscalAddress customer fiscal address
 * @param postalCode customer postal code
 * @param city customer city
 * @param countryCode customer country code
 */
public record CreateCustomerRequest(
    @NotNull UUID identityUserId,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 150) String lastName,
    @NotNull DocumentType documentType,
    @NotBlank @Size(max = 50) String documentNumber,
    @NotNull @Past LocalDate dateOfBirth,
    @Size(max = 30) String phoneNumber,
    @Size(max = 255) String fiscalAddress,
    @Size(max = 20) String postalCode,
    @Size(max = 100) String city,
    @Size(min = 2, max = 2) String countryCode) {}
