package com.pbbaque.banking.audit.repository;

import com.pbbaque.banking.audit.domain.AuditEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for persisted audit events. */
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

  /**
   * Finds all audit events for a specific aggregate.
   *
   * @param aggregateType aggregate type
   * @param aggregateId aggregate identifier
   * @return matching audit events
   */
  List<AuditEvent> findAllByAggregateTypeAndAggregateId(String aggregateType, String aggregateId);

  /**
   * Finds all audit events created by a specific actor.
   *
   * @param actorId actor identifier
   * @return matching audit events
   */
  List<AuditEvent> findAllByActorId(UUID actorId);

  /**
   * Finds all audit events of a specific type.
   *
   * @param eventType event type
   * @return matching audit events
   */
  List<AuditEvent> findAllByEventType(String eventType);
}
