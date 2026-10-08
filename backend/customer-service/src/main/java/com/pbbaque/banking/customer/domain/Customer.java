package com.pbbaque.banking.customer.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.pbbaque.banking.customer.exception.InvalidCustomerStatusTransitionException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "CUSTOMERS")
public class Customer {
    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "ID", nullable = false, unique = true, columnDefinition = "RAW(16)")
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "IDENTITY_USER_ID", nullable = false, unique = true, columnDefinition = "RAW(16)")
    private UUID identityUserId;

    @Column(name = "FIRST_NAME", nullable = false, length = 100)
    private String firstName;

    @Column(name = "LAST_NAME", nullable = false, length = 150)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "DOCUMENT_TYPE", nullable = false, length = 20)
    private DocumentType documentType;

    @Column(name = "DOCUMENT_NUMBER", nullable = false, length = 50)
    private String documentNumber;

    @Column(name = "DATE_OF_BIRTH", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "PHONE_NUMBER", length = 30)
    private String phoneNumber;

    @Column(name = "FISCAL_ADDRESS", length = 255)
    private String fiscalAddress;

    @Column(name = "POSTAL_CODE", length = 20)
    private String postalCode;

    @Column(name = "CITY", length = 100)
    private String city;

    @Column(name = "COUNTRY_CODE", length = 2, columnDefinition = "CHAR(2)")
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 30)
    private CustomerStatus status;

    @Column(name = "CREATED_AT", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "DELETED_AT")
    private OffsetDateTime deletedAt;

    protected Customer() {
    }

    private Customer(
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
            OffsetDateTime updatedAt,
            OffsetDateTime deletedAt) {
        this.id = id;
        this.identityUserId = identityUserId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.dateOfBirth = dateOfBirth;
        this.phoneNumber = phoneNumber;
        this.fiscalAddress = fiscalAddress;
        this.postalCode = postalCode;
        this.city = city;
        this.countryCode = countryCode;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Customer create(
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
            String countryCode) {

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        return new Customer(
                UUID.randomUUID(),
                identityUserId,
                firstName,
                lastName,
                documentType,
                documentNumber,
                dateOfBirth,
                phoneNumber,
                fiscalAddress,
                postalCode,
                city,
                countryCode,
                CustomerStatus.PENDING_VERIFICATION,
                now,
                now,
                null);
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdentityUserId() {
        return identityUserId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getFiscalAddress() {
        return fiscalAddress;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCity() {
        return city;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void updateContactData(
            String phoneNumber,
            String fiscalAddress,
            String postalCode,
            String city,
            String countryCode) {
        this.phoneNumber = phoneNumber;
        this.fiscalAddress = fiscalAddress;
        this.postalCode = postalCode;
        this.city = city;
        this.countryCode = countryCode;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void archive() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        this.status = CustomerStatus.INACTIVE;
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public void activate() {
        if (status != CustomerStatus.PENDING_VERIFICATION
                && status != CustomerStatus.BLOCKED) {
            throw new InvalidCustomerStatusTransitionException(
                    status,
                    CustomerStatus.ACTIVE);
        }

        this.status = CustomerStatus.ACTIVE;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void block() {
        if (status != CustomerStatus.ACTIVE) {
            throw new InvalidCustomerStatusTransitionException(
                    status,
                    CustomerStatus.BLOCKED);
        }

        this.status = CustomerStatus.BLOCKED;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void deactivate() {
        if (status != CustomerStatus.ACTIVE) {
            throw new InvalidCustomerStatusTransitionException(
                    status,
                    CustomerStatus.INACTIVE);
        }

        this.status = CustomerStatus.INACTIVE;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

}
