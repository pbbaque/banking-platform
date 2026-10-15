package com.pbbaque.banking.customer.dto;

import jakarta.validation.constraints.Size;

/**
 * Request payload for updating customer contact information.
 *
 * @param phoneNumber customer phone number
 * @param fiscalAddress customer fiscal address
 * @param postalCode customer postal code
 * @param city customer city
 * @param countryCode customer country code
 */
public record UpdateCustomerContactRequest(
    @Size(max = 30) String phoneNumber,
    @Size(max = 255) String fiscalAddress,
    @Size(max = 20) String postalCode,
    @Size(max = 100) String city,
    @Size(min = 2, max = 2) String countryCode) {}
