package com.pbbaque.banking.customer.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.pbbaque.banking.customer.domain.DocumentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

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

        @Size(min = 2, max = 2) String countryCode) {
}
