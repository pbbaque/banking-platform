package com.pbbaque.banking.customer.service;

import com.pbbaque.banking.customer.domain.Customer;
import com.pbbaque.banking.customer.dto.CreateCustomerRequest;
import com.pbbaque.banking.customer.dto.CustomerResponse;
import com.pbbaque.banking.customer.dto.UpdateCustomerContactRequest;
import com.pbbaque.banking.customer.exception.CustomerAlreadyExistsException;
import com.pbbaque.banking.customer.exception.CustomerNotFoundException;
import com.pbbaque.banking.customer.repository.CustomerRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service responsible for customer management operations. */
@Service
public class CustomerService {

  private final CustomerRepository customerRepository;

  public CustomerService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  /**
   * Retrieves all non-archived customers.
   *
   * @return list of active customer records
   */
  @Transactional(readOnly = true)
  public List<CustomerResponse> getAll() {
    return customerRepository.findAllByDeletedAtIsNull().stream()
        .map(CustomerResponse::from)
        .toList();
  }

  /**
   * Retrieves a non-archived customer by identifier.
   *
   * @param id customer identifier
   * @return matching customer
   * @throws CustomerNotFoundException if the customer does not exist
   */
  @Transactional(readOnly = true)
  public CustomerResponse getById(UUID id) {
    Customer customer =
        customerRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));

    return CustomerResponse.from(customer);
  }

  /**
   * Creates a new customer.
   *
   * @param request customer creation request
   * @return created customer
   * @throws CustomerAlreadyExistsException if the identity user or document is already associated
   *     with a customer
   */
  @Transactional
  public CustomerResponse create(CreateCustomerRequest request) {
    if (customerRepository.existsByIdentityUserId(request.identityUserId())) {
      throw new CustomerAlreadyExistsException(
          "A customer alredy exists for identity user: " + request.identityUserId());
    }

    if (customerRepository.existsByDocumentTypeAndDocumentNumber(
        request.documentType(), request.documentNumber())) {
      throw new CustomerAlreadyExistsException(
          "A customer already exists with document: "
              + request.documentType()
              + " "
              + request.documentNumber());
    }

    Customer customer =
        Customer.create(
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

  /**
   * Updates the contact information of an existing customer.
   *
   * @param id customer identifier
   * @param request contact data update request
   * @return updated customer
   * @throws CustomerNotFoundException if the customer does not exist
   */
  @Transactional
  public CustomerResponse updateContactData(UUID id, UpdateCustomerContactRequest request) {
    Customer customer =
        customerRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));

    customer.updateContactData(
        request.phoneNumber(),
        request.fiscalAddress(),
        request.postalCode(),
        request.city(),
        request.countryCode());

    return CustomerResponse.from(customer);
  }

  /**
   * Archives an existing customer.
   *
   * @param id customer identifier
   * @throws CustomerNotFoundException if the customer does not exist
   */
  @Transactional
  public void archive(UUID id) {
    Customer customer =
        customerRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));

    customer.archive();
  }

  /**
   * Activates an existing customer.
   *
   * @param id customer identifier
   * @return activated customer
   * @throws CustomerNotFoundException if the customer does not exist
   */
  @Transactional
  public CustomerResponse activate(UUID id) {
    Customer customer = findActiveCustomer(id);

    customer.activate();

    return CustomerResponse.from(customer);
  }

  /**
   * Blocks an existing customer.
   *
   * @param id customer identifier
   * @return blocked customer
   * @throws CustomerNotFoundException if the customer does not exist
   */
  @Transactional
  public CustomerResponse block(UUID id) {
    Customer customer = findActiveCustomer(id);

    customer.block();

    return CustomerResponse.from(customer);
  }

  /**
   * Deactivates an existing customer.
   *
   * @param id customer identifier
   * @return deactivated customer
   * @throws CustomerNotFoundException if the customer does not exist
   */
  @Transactional
  public CustomerResponse deactivate(UUID id) {
    Customer customer = findActiveCustomer(id);

    customer.deactivate();

    return CustomerResponse.from(customer);
  }

  private Customer findActiveCustomer(UUID id) {
    return customerRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));
  }
}
