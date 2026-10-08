package com.pbbaque.banking.customer.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.pbbaque.banking.customer.domain.Customer;
import com.pbbaque.banking.customer.domain.CustomerStatus;
import com.pbbaque.banking.customer.domain.DocumentType;

public record CustomerResponse(
        UUID id,
        UUID identityUserId,
        String firstName,
        String lastName,
        DocumentType documentType,
        String documentNumber,
        LocalDate dateOfBirth,
        String phoneNumber,
        String fiscalAddress,
        String postalCode,
        String city,
        String countryCode,
        CustomerStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getIdentityUserId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getDocumentType(),
                customer.getDocumentNumber(),
                customer.getDateOfBirth(),
                customer.getPhoneNumber(),
                customer.getFiscalAddress(),
                customer.getPostalCode(),
                customer.getCity(),
                customer.getCountryCode(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt());
    }
}
