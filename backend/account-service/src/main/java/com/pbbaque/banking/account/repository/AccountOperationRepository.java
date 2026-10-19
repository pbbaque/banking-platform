package com.pbbaque.banking.account.repository;

import com.pbbaque.banking.account.domain.AccountOperation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Provides persistence operations for idempotent account balance operations. */
public interface AccountOperationRepository extends JpaRepository<AccountOperation, UUID> {}
