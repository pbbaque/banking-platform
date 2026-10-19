package com.pbbaque.banking.transfer.domain;

/** Represents the publication state of an outbox event. */
public enum OutboxEventStatus {
  PENDING,
  PUBLISHED
}
