package com.pbbaque.banking.transfer.repository;

import com.pbbaque.banking.transfer.domain.OutboxEvent;
import com.pbbaque.banking.transfer.domain.OutboxEventStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for durable transfer outbox events. */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

  /**
   * Finds publishable events ordered by creation time.
   *
   * @param status required publication status
   * @param availableAt latest availability timestamp
   * @param pageable maximum number of events to retrieve
   * @return matching outbox events
   */
  List<OutboxEvent> findByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
      OutboxEventStatus status, OffsetDateTime availableAt, Pageable pageable);
}
