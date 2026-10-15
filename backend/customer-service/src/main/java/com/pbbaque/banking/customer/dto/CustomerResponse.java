package com.pbbaque.banking.customer.dto;

import com.pbbaque.banking.customer.domain.Customer;
import com.pbbaque.banking.customer.domain.CustomerStatus;
import com.pbbaque.banking.customer.domain.DocumentType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response representation of a banking customer.
 *
 * @param id customer identifier
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
 * @param status current customer status
 * @param createdAt creation timestamp
 * @param updatedAt last update timestamp
 */
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

  /**
   * Creates a response representation from a customer entity.
   *
   * @param customer customer entity
   * @return customer response
   */
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
