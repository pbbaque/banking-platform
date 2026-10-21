package com.pbbaque.banking.customer.repository;

import com.pbbaque.banking.customer.domain.Customer;
import com.pbbaque.banking.customer.domain.DocumentType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for accessing and querying customer persistence data. */
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

  List<Customer> findAllByDeletedAtIsNull();

  Optional<Customer> findByIdAndDeletedAtIsNull(UUID id);

  Optional<Customer> findByIdentityUserId(UUID userId);

  Optional<Customer> findByIdentityUserIdAndDeletedAtIsNull(UUID identityUserId);

  Optional<Customer> findByDocumentTypeAndDocumentNumber(
      DocumentType documentType, String documentNumber);

  boolean existsByIdentityUserId(UUID userId);

  boolean existsByDocumentTypeAndDocumentNumber(DocumentType documentType, String documentNumber);
}
