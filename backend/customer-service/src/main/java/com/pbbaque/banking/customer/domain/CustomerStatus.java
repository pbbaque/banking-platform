package com.pbbaque.banking.customer.domain;

/** Represents the lifecycle status of a banking customer. */
public enum CustomerStatus {
  PENDING_VERIFICATION,
  ACTIVE,
  BLOCKED,
  INACTIVE
}
