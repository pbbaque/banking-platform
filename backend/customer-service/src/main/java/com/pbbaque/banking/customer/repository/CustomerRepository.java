package com.pbbaque.banking.customer.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pbbaque.banking.customer.domain.Customer;
import com.pbbaque.banking.customer.domain.DocumentType;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    List<Customer> findAllByDeletedAtIsNull();

    Optional<Customer> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Customer> findByIdentityUserId(UUID userId);

    Optional<Customer> findByDocumentTypeAndDocumentNumber(DocumentType documentType, String documentNumber);

    boolean existsByIdentityUserId(UUID userId);

    boolean existsByDocumentTypeAndDocumentNumber(DocumentType documentType, String documentNumber);

}
