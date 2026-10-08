package com.pbbaque.banking.customer.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pbbaque.banking.customer.dto.CreateCustomerRequest;
import com.pbbaque.banking.customer.dto.CustomerResponse;
import com.pbbaque.banking.customer.dto.UpdateCustomerContactRequest;
import com.pbbaque.banking.customer.service.CustomerService;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAll() {
        return ResponseEntity.ok(customerService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getById(@PathVariable UUID id) {
        CustomerResponse response = customerService.getById(id);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse response = customerService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/contact")
    public ResponseEntity<CustomerResponse> updateContactData(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerContactRequest request) {
        CustomerResponse response = customerService.updateContactData(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        customerService.archive(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<CustomerResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.activate(id));
    }

    @PatchMapping("/{id}/block")
    public ResponseEntity<CustomerResponse> block(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.block(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<CustomerResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.deactivate(id));
    }

}
