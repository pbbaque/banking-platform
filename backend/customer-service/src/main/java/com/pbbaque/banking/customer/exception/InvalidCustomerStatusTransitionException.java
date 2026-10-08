package com.pbbaque.banking.customer.exception;

import com.pbbaque.banking.customer.domain.CustomerStatus;

public class InvalidCustomerStatusTransitionException extends RuntimeException {

    public InvalidCustomerStatusTransitionException(
            CustomerStatus currentStatus,
            CustomerStatus targetStatus) {
        super(
                "Invalid customer status transition from "
                        + currentStatus
                        + " to "
                        + targetStatus);
    }
}