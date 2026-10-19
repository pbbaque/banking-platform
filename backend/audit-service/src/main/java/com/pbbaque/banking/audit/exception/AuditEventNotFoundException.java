package com.pbbaque.banking.audit.exception;

import java.util.UUID;

/** Exception thrown when an audit event cannot be found. */
public class AuditEventNotFoundException extends RuntimeException {

  /**
   * Creates a new audit event not found exception.
   *
   * @param id audit event identifier
   */
  public AuditEventNotFoundException(UUID id) {
    super("Audit event not found: " + id);
  }
}
