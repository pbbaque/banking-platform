package com.pbbaque.banking.customer.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pbbaque.banking.customer.domain.Customer;
import com.pbbaque.banking.customer.dto.CreateCustomerRequest;
import com.pbbaque.banking.customer.dto.CustomerResponse;
import com.pbbaque.banking.customer.dto.UpdateCustomerContactRequest;
import com.pbbaque.banking.customer.exception.CustomerAlreadyExistsException;
import com.pbbaque.banking.customer.exception.CustomerNotFoundException;
import com.pbbaque.banking.customer.repository.CustomerRepository;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAll() {
        return customerRepository.findAllByDeletedAtIsNull()
                .stream()
                .map(CustomerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(UUID id) {
        Customer customer = customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with id: " + id));

        return CustomerResponse.from(customer);
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        if (customerRepository.existsByIdentityUserId(request.identityUserId())) {
            throw new CustomerAlreadyExistsException(
                    "Acustomer alredy exists for identity user: " + request.identityUserId());
        }

        if (customerRepository.existsByDocumentTypeAndDocumentNumber(request.documentType(),
                request.documentNumber())) {
            throw new CustomerAlreadyExistsException(
                    "A customer already exists with document: "
                            + request.documentType()
                            + " "
                            + request.documentNumber());
        }

        Customer customer = Customer.create(
                request.identityUserId(),
                request.firstName(),
                request.lastName(),
                request.documentType(),
                request.documentNumber(),
                request.dateOfBirth(),
                request.phoneNumber(),
                request.fiscalAddress(),
                request.postalCode(),
                request.city(),
                request.countryCode());

        Customer savedCustomer = customerRepository.save(customer);

        return CustomerResponse.from(savedCustomer);
    }

    @Transactional
    public CustomerResponse updateContactData(
            UUID id,
            UpdateCustomerContactRequest request) {
        Customer customer = customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with id: " + id));

        customer.updateContactData(
                request.phoneNumber(),
                request.fiscalAddress(),
                request.postalCode(),
                request.city(),
                request.countryCode());

        return CustomerResponse.from(customer);
    }

    @Transactional
    public void archive(UUID id) {
        Customer customer = customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with id: " + id));

        customer.archive();
    }

    @Transactional
    public CustomerResponse activate(UUID id) {
        Customer customer = findActiveCustomer(id);

        customer.activate();

        return CustomerResponse.from(customer);
    }

    @Transactional
    public CustomerResponse block(UUID id) {
        Customer customer = findActiveCustomer(id);

        customer.block();

        return CustomerResponse.from(customer);
    }

    @Transactional
    public CustomerResponse deactivate(UUID id) {
        Customer customer = findActiveCustomer(id);

        customer.deactivate();

        return CustomerResponse.from(customer);
    }

    private Customer findActiveCustomer(UUID id) {
        return customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with id: " + id));
    }
}
