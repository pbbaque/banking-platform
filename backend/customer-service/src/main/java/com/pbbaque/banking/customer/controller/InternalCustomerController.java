package com.pbbaque.banking.customer.controller;

import com.pbbaque.banking.customer.dto.CustomerResponse;
import com.pbbaque.banking.customer.service.CustomerService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Internal endpoints used by other banking-platform services. */
@RestController
@RequestMapping("/api/internal/customers")
public class InternalCustomerController {

  private final CustomerService customerService;

  /**
   * Creates the internal customer controller.
   *
   * @param customerService customer application service
   */
  public InternalCustomerController(CustomerService customerService) {
    this.customerService = customerService;
  }

  /**
   * Finds the customer associated with an identity-service user.
   *
   * @param identityUserId identity-service user identifier
   * @return associated customer
   */
  @GetMapping("/by-identity-user/{identityUserId}")
  public ResponseEntity<CustomerResponse> getByIdentityUserId(@PathVariable UUID identityUserId) {

    return ResponseEntity.ok(customerService.getByIdentityUserId(identityUserId));
  }
}
