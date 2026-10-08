package com.pbbaque.banking.customer.repository;

import com.pbbaque.banking.customer.domain.Customer;
import com.pbbaque.banking.customer.domain.DocumentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("local")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerRepositoryIT {

        @Autowired
        private CustomerRepository customerRepository;

        private Customer createCustomer(
                        UUID identityUserId,
                        String documentNumber) {
                return Customer.create(
                                identityUserId,
                                "Integration",
                                "Test",
                                DocumentType.DNI,
                                documentNumber,
                                LocalDate.of(1990, 1, 1),
                                "600000000",
                                "Calle Integration 1",
                                "48001",
                                "Bilbao",
                                "ES");
        }

        @Test
        void shouldSaveAndFindCustomerById() {
                Customer customer = createCustomer(
                                UUID.randomUUID(),
                                "10000001A");

                Customer savedCustomer = customerRepository.saveAndFlush(customer);

                Optional<Customer> result = customerRepository.findByIdAndDeletedAtIsNull(
                                savedCustomer.getId());

                assertTrue(result.isPresent());
                assertEquals(savedCustomer.getId(), result.get().getId());
                assertEquals(
                                savedCustomer.getIdentityUserId(),
                                result.get().getIdentityUserId());
        }

        @Test
        void shouldFindCustomerByIdentityUserId() {
                UUID identityUserId = UUID.randomUUID();

                Customer customer = createCustomer(
                                identityUserId,
                                "10000002B");

                customerRepository.saveAndFlush(customer);

                Optional<Customer> result = customerRepository.findByIdentityUserId(identityUserId);

                assertTrue(result.isPresent());
                assertEquals(identityUserId, result.get().getIdentityUserId());
        }

        @Test
        void shouldFindCustomerByDocument() {
                Customer customer = createCustomer(
                                UUID.randomUUID(),
                                "10000003C");

                customerRepository.saveAndFlush(customer);

                Optional<Customer> result = customerRepository.findByDocumentTypeAndDocumentNumber(
                                DocumentType.DNI,
                                "10000003C");

                assertTrue(result.isPresent());
                assertEquals(
                                "10000003C",
                                result.get().getDocumentNumber());
        }

        @Test
        void shouldDetectExistingIdentityUserId() {
                UUID identityUserId = UUID.randomUUID();

                Customer customer = createCustomer(
                                identityUserId,
                                "10000004D");

                customerRepository.saveAndFlush(customer);

                boolean exists = customerRepository.existsByIdentityUserId(identityUserId);

                assertTrue(exists);
        }

        @Test
        void shouldDetectExistingDocument() {
                Customer customer = createCustomer(
                                UUID.randomUUID(),
                                "10000005E");

                customerRepository.saveAndFlush(customer);

                boolean exists = customerRepository.existsByDocumentTypeAndDocumentNumber(
                                DocumentType.DNI,
                                "10000005E");

                assertTrue(exists);
        }

        @Test
        void shouldExcludeArchivedCustomerWhenFindingById() {
                Customer customer = createCustomer(
                                UUID.randomUUID(),
                                "10000006F");

                Customer savedCustomer = customerRepository.saveAndFlush(customer);

                savedCustomer.archive();

                customerRepository.flush();

                Optional<Customer> result = customerRepository.findByIdAndDeletedAtIsNull(
                                savedCustomer.getId());

                assertTrue(result.isEmpty());
        }

        @Test
        void shouldReturnOnlyNonArchivedCustomers() {
                Customer activeCustomer = createCustomer(
                                UUID.randomUUID(),
                                "10000007G");

                Customer archivedCustomer = createCustomer(
                                UUID.randomUUID(),
                                "10000008H");

                Customer savedActiveCustomer = customerRepository.save(activeCustomer);

                Customer savedArchivedCustomer = customerRepository.save(archivedCustomer);

                savedArchivedCustomer.archive();

                customerRepository.flush();

                List<Customer> customers = customerRepository.findAllByDeletedAtIsNull();

                assertTrue(
                                customers.stream()
                                                .anyMatch(customer -> customer.getId()
                                                                .equals(savedActiveCustomer.getId())));

                assertTrue(
                                customers.stream()
                                                .noneMatch(customer -> customer.getId()
                                                                .equals(savedArchivedCustomer.getId())));
        }
}