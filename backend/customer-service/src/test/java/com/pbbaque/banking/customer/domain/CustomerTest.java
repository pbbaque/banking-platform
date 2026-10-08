package com.pbbaque.banking.customer.domain;

import com.pbbaque.banking.customer.exception.InvalidCustomerStatusTransitionException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CustomerTest {

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
    void shouldCreateCustomerWithPendingVerificationStatus() {
        Customer customer = createCustomer();

        assertNotNull(customer.getId());
        assertEquals(CustomerStatus.PENDING_VERIFICATION, customer.getStatus());
        assertNotNull(customer.getCreatedAt());
        assertNotNull(customer.getUpdatedAt());
        assertNull(customer.getDeletedAt());
    }

    @Test
    void shouldActivatePendingCustomer() {
        Customer customer = createCustomer();

        customer.activate();

        assertEquals(CustomerStatus.ACTIVE, customer.getStatus());
    }

    @Test
    void shouldBlockActiveCustomer() {
        Customer customer = createCustomer();

        customer.activate();
        customer.block();

        assertEquals(CustomerStatus.BLOCKED, customer.getStatus());
    }

    @Test
    void shouldReactivateBlockedCustomer() {
        Customer customer = createCustomer();

        customer.activate();
        customer.block();
        customer.activate();

        assertEquals(CustomerStatus.ACTIVE, customer.getStatus());
    }

    @Test
    void shouldDeactivateActiveCustomer() {
        Customer customer = createCustomer();

        customer.activate();
        customer.deactivate();

        assertEquals(CustomerStatus.INACTIVE, customer.getStatus());
    }

    @Test
    void shouldRejectActivationOfInactiveCustomer() {
        Customer customer = createCustomer();

        customer.activate();
        customer.deactivate();

        InvalidCustomerStatusTransitionException exception = assertThrows(
                InvalidCustomerStatusTransitionException.class,
                customer::activate);

        assertTrue(
                exception.getMessage()
                        .contains("INACTIVE"));

        assertEquals(CustomerStatus.INACTIVE, customer.getStatus());
    }

    @Test
    void shouldArchiveCustomer() {
        Customer customer = createCustomer();

        customer.activate();

        var previousUpdatedAt = customer.getUpdatedAt();

        customer.archive();

        assertEquals(CustomerStatus.INACTIVE, customer.getStatus());
        assertNotNull(customer.getDeletedAt());
        assertNotNull(customer.getUpdatedAt());
        assertTrue(
                customer.getUpdatedAt()
                        .isAfter(previousUpdatedAt)
                        || customer.getUpdatedAt().isEqual(previousUpdatedAt));
    }

    @Test
    void shouldRejectBlockingPendingCustomer() {
        Customer customer = createCustomer();

        assertThrows(
                InvalidCustomerStatusTransitionException.class,
                customer::block);

        assertEquals(CustomerStatus.PENDING_VERIFICATION, customer.getStatus());
    }

    @Test
    void shouldRejectDeactivatingPendingCustomer() {
        Customer customer = createCustomer();

        assertThrows(
                InvalidCustomerStatusTransitionException.class,
                customer::deactivate);

        assertEquals(CustomerStatus.PENDING_VERIFICATION, customer.getStatus());
    }
}