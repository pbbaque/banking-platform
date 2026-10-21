package com.pbbaque.banking.transfer.domain;

/** Internal processing stage used to safely resume interrupted transfers. */
public enum TransferProcessingStage {

  /** The source account debit has not yet been confirmed. */
  DEBIT_PENDING,

  /** The source debit succeeded and the destination credit is pending. */
  CREDIT_PENDING,

  /** The destination credit failed and source compensation is pending. */
  COMPENSATION_PENDING,

  /** The transfer reached a final completed or failed state. */
  TERMINAL,

  /** The processing stage of a legacy pending transfer cannot be determined safely. */
  RECOVERY_REQUIRED
}
