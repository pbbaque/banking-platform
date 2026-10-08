package com.pbbaque.banking.customer.service;

import com.pbbaque.banking.customer.domain.Customer;
import com.pbbaque.banking.customer.domain.CustomerStatus;
import com.pbbaque.banking.customer.domain.DocumentType;
import com.pbbaque.banking.customer.dto.CreateCustomerRequest;
import com.pbbaque.banking.customer.dto.CustomerResponse;
import com.pbbaque.banking.customer.dto.UpdateCustomerContactRequest;
import com.pbbaque.banking.customer.exception.CustomerAlreadyExistsException;
import com.pbbaque.banking.customer.exception.CustomerNotFoundException;
import com.pbbaque.banking.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

        @Mock
        private CustomerRepository customerRepository;

        private CustomerService customerService;

        @BeforeEach
        void setUp() {
                customerService = new CustomerService(customerRepository);
        }

        private CreateCustomerRequest createRequest() {
                return new CreateCustomerRequest(
                                UUID.randomUUID(),
                                "Test",
                                "Customer",
                                DocumentType.DNI,
                                "12345678A",
                                LocalDate.of(1990, 1, 1),
                                "600000000",
                                "Calle Test 1",
                                "48001",
                                "Bilbao",
                                "ES");
        }

        private Customer createCustomer() {
                return Customer.create(
                                UUID.randomUUID(),
                                "Test",
                                "Customer",
                                DocumentType.DNI,
                                "12345678A",
                                LocalDate.of(1990, 1, 1),
                                "600000000",
                                "Calle Test 1",
                                "48001",
                                "Bilbao",
                                "ES");
        }

        @Test
        void shouldCreateCustomer() {
                CreateCustomerRequest request = createRequest();

                when(customerRepository.existsByIdentityUserId(request.identityUserId()))
                                .thenReturn(false);

                when(customerRepository.existsByDocumentTypeAndDocumentNumber(
                                request.documentType(),
                                request.documentNumber())).thenReturn(false);

                when(customerRepository.save(any(Customer.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                CustomerResponse response = customerService.create(request);

                assertNotNull(response.id());
                assertEquals(request.identityUserId(), response.identityUserId());
                assertEquals(CustomerStatus.PENDING_VERIFICATION, response.status());

                verify(customerRepository).save(any(Customer.class));
        }

        @Test
        void shouldRejectDuplicateIdentityUserId() {
                CreateCustomerRequest request = createRequest();

                when(customerRepository.existsByIdentityUserId(request.identityUserId()))
                                .thenReturn(true);

                assertThrows(
                                CustomerAlreadyExistsException.class,
                                () -> customerService.create(request));

                verify(customerRepository, never()).save(any(Customer.class));
        }

        @Test
        void shouldRejectDuplicateDocument() {
                CreateCustomerRequest request = createRequest();

                when(customerRepository.existsByIdentityUserId(request.identityUserId()))
                                .thenReturn(false);

                when(customerRepository.existsByDocumentTypeAndDocumentNumber(
                                request.documentType(),
                                request.documentNumber())).thenReturn(true);

                assertThrows(
                                CustomerAlreadyExistsException.class,
                                () -> customerService.create(request));

                verify(customerRepository, never()).save(any(Customer.class));
        }

        @Test
        void shouldGetCustomerById() {
                Customer customer = createCustomer();

                when(customerRepository.findByIdAndDeletedAtIsNull(customer.getId()))
                                .thenReturn(Optional.of(customer));

                CustomerResponse response = customerService.getById(customer.getId());

                assertEquals(customer.getId(), response.id());
                assertEquals(customer.getFirstName(), response.firstName());
        }

        @Test
        void shouldThrowWhenCustomerDoesNotExist() {
                UUID id = UUID.randomUUID();

                when(customerRepository.findByIdAndDeletedAtIsNull(id))
                                .thenReturn(Optional.empty());

                assertThrows(
                                CustomerNotFoundException.class,
                                () -> customerService.getById(id));
        }

        @Test
        void shouldUpdateCustomerContactData() {
                Customer customer = createCustomer();

                when(customerRepository.findByIdAndDeletedAtIsNull(customer.getId()))
                                .thenReturn(Optional.of(customer));

                UpdateCustomerContactRequest request = new UpdateCustomerContactRequest(
                                "611111111",
                                "Nueva direccion 10",
                                "48002",
                                "Bilbao",
                                "ES");

                CustomerResponse response = customerService.updateContactData(
                                customer.getId(),
                                request);

                assertEquals("611111111", response.phoneNumber());
                assertEquals("Nueva direccion 10", response.fiscalAddress());
                assertEquals("48002", response.postalCode());
                assertEquals("Bilbao", response.city());
                assertEquals("ES", response.countryCode());
        }

        @Test
        void shouldThrowWhenUpdatingContactOfNonExistingCustomer() {
                UUID id = UUID.randomUUID();

                when(customerRepository.findByIdAndDeletedAtIsNull(id))
                                .thenReturn(Optional.empty());

                UpdateCustomerContactRequest request = new UpdateCustomerContactRequest(
                                "611111111",
                                "Nueva direccion",
                                "48002",
                                "Bilbao",
                                "ES");

                assertThrows(
                                CustomerNotFoundException.class,
                                () -> customerService.updateContactData(id, request));
        }

        @Test
        void shouldReturnAllNonArchivedCustomers() {
                Customer customer1 = createCustomer();

                Customer customer2 = Customer.create(
                                UUID.randomUUID(),
                                "Another",
                                "Customer",
                                DocumentType.PASSPORT,
                                "X1234567",
                                LocalDate.of(1985, 5, 15),
                                null,
                                null,
                                null,
                                null,
                                "ES");

                when(customerRepository.findAllByDeletedAtIsNull())
                                .thenReturn(List.of(customer1, customer2));

                List<CustomerResponse> result = customerService.getAll();

                assertEquals(2, result.size());
                assertEquals(customer1.getId(), result.get(0).id());
                assertEquals(customer2.getId(), result.get(1).id());
        }

        @Test
        void shouldActivateCustomer() {
                Customer customer = createCustomer();

                when(customerRepository.findByIdAndDeletedAtIsNull(customer.getId()))
                                .thenReturn(Optional.of(customer));

                CustomerResponse response = customerService.activate(customer.getId());

                assertEquals(CustomerStatus.ACTIVE, response.status());
        }

        @Test
        void shouldThrowWhenActivatingNonExistingCustomer() {
                UUID id = UUID.randomUUID();

                when(customerRepository.findByIdAndDeletedAtIsNull(id))
                                .thenReturn(Optional.empty());

                assertThrows(
                                CustomerNotFoundException.class,
                                () -> customerService.activate(id));
        }

        @Test
        void shouldBlockCustomer() {
                Customer customer = createCustomer();
                customer.activate();

                when(customerRepository.findByIdAndDeletedAtIsNull(customer.getId()))
                                .thenReturn(Optional.of(customer));

                CustomerResponse response = customerService.block(customer.getId());

                assertEquals(CustomerStatus.BLOCKED, response.status());
        }

        @Test
        void shouldThrowWhenBlockingNonExistingCustomer() {
                UUID id = UUID.randomUUID();

                when(customerRepository.findByIdAndDeletedAtIsNull(id))
                                .thenReturn(Optional.empty());

                assertThrows(
                                CustomerNotFoundException.class,
                                () -> customerService.block(id));
        }

        @Test
        void shouldDeactivateCustomer() {
                Customer customer = createCustomer();
                customer.activate();

                when(customerRepository.findByIdAndDeletedAtIsNull(customer.getId()))
                                .thenReturn(Optional.of(customer));

                CustomerResponse response = customerService.deactivate(customer.getId());

                assertEquals(CustomerStatus.INACTIVE, response.status());
        }

        @Test
        void shouldThrowWhenDeactivatingNonExistingCustomer() {
                UUID id = UUID.randomUUID();

                when(customerRepository.findByIdAndDeletedAtIsNull(id))
                                .thenReturn(Optional.empty());

                assertThrows(
                                CustomerNotFoundException.class,
                                () -> customerService.deactivate(id));
        }

        @Test
        void shouldArchiveCustomer() {
                Customer customer = createCustomer();

                when(customerRepository.findByIdAndDeletedAtIsNull(customer.getId()))
                                .thenReturn(Optional.of(customer));

                customerService.archive(customer.getId());

                assertEquals(CustomerStatus.INACTIVE, customer.getStatus());
                assertNotNull(customer.getDeletedAt());
        }

        @Test
        void shouldThrowWhenArchivingNonExistingCustomer() {
                UUID id = UUID.randomUUID();

                when(customerRepository.findByIdAndDeletedAtIsNull(id))
                                .thenReturn(Optional.empty());

                assertThrows(
                                CustomerNotFoundException.class,
                                () -> customerService.archive(id));
        }
}