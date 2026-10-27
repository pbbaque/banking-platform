package com.pbbaque.banking.audit.repository;

import com.pbbaque.banking.audit.domain.AuditEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for persisted audit events. */
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

  /**
   * Finds an audit event owned by a specific actor.
   *
   * @param id audit event identifier
   * @param actorId actor identifier
   * @return matching audit event when it exists
   */
  Optional<AuditEvent> findByIdAndActorId(UUID id, UUID actorId);

  /**
   * Finds audit events for an aggregate and actor.
   *
   * @param aggregateType aggregate type
   * @param aggregateId aggregate identifier
   * @param actorId actor identifier
   * @return matching audit events
   */
  List<AuditEvent> findAllByAggregateTypeAndAggregateIdAndActorId(
      String aggregateType, String aggregateId, UUID actorId);

  /**
   * Finds all audit events created by a specific actor.
   *
   * @param actorId actor identifier
   * @return matching audit events
   */
  List<AuditEvent> findAllByActorId(UUID actorId);

  /**
   * Finds audit events of a specific type for an actor.
   *
   * @param eventType event type
   * @param actorId actor identifier
   * @return matching audit events
   */
  List<AuditEvent> findAllByEventTypeAndActorId(String eventType, UUID actorId);
}
