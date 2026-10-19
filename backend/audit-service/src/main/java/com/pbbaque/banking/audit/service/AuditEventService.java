package com.pbbaque.banking.audit.service;

import com.pbbaque.banking.audit.domain.AuditEvent;
import com.pbbaque.banking.audit.dto.AuditEventResponse;
import com.pbbaque.banking.audit.dto.CreateAuditEventRequest;
import com.pbbaque.banking.audit.exception.AuditEventNotFoundException;
import com.pbbaque.banking.audit.repository.AuditEventRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service responsible for creating and retrieving audit events. */
@Service
public class AuditEventService {

  private final AuditEventRepository auditEventRepository;

  /**
   * Creates a new audit event service.
   *
   * @param auditEventRepository audit event repository
   */
  public AuditEventService(AuditEventRepository auditEventRepository) {
    this.auditEventRepository = auditEventRepository;
  }

  /**
   * Creates and persists a new audit event.
   *
   * @param request audit event creation request
   * @return persisted audit event
   */
  @Transactional
  public AuditEventResponse create(CreateAuditEventRequest request) {
    AuditEvent auditEvent =
        AuditEvent.create(
            request.eventType(),
            request.aggregateType(),
            request.aggregateId(),
            request.actorId(),
            request.outcome(),
            request.description(),
            request.metadata(),
            request.occurredAt());

    AuditEvent savedAuditEvent = auditEventRepository.save(auditEvent);

    return AuditEventResponse.from(savedAuditEvent);
  }

  /**
   * Finds an audit event by identifier.
   *
   * @param id audit event identifier
   * @return audit event
   * @throws AuditEventNotFoundException if the audit event does not exist
   */
  @Transactional(readOnly = true)
  public AuditEventResponse getById(UUID id) {
    AuditEvent auditEvent =
        auditEventRepository.findById(id).orElseThrow(() -> new AuditEventNotFoundException(id));

    return AuditEventResponse.from(auditEvent);
  }

  /**
   * Finds audit events for a specific aggregate.
   *
   * @param aggregateType aggregate type
   * @param aggregateId aggregate identifier
   * @return matching audit events
   */
  @Transactional(readOnly = true)
  public List<AuditEventResponse> getByAggregate(String aggregateType, String aggregateId) {
    return auditEventRepository
        .findAllByAggregateTypeAndAggregateId(aggregateType, aggregateId)
        .stream()
        .map(AuditEventResponse::from)
        .toList();
  }

  /**
   * Finds audit events created by a specific actor.
   *
   * @param actorId actor identifier
   * @return matching audit events
   */
  @Transactional(readOnly = true)
  public List<AuditEventResponse> getByActor(UUID actorId) {
    return auditEventRepository.findAllByActorId(actorId).stream()
        .map(AuditEventResponse::from)
        .toList();
  }

  /**
   * Finds audit events of a specific event type.
   *
   * @param eventType event type
   * @return matching audit events
   */
  @Transactional(readOnly = true)
  public List<AuditEventResponse> getByEventType(String eventType) {
    return auditEventRepository.findAllByEventType(eventType).stream()
        .map(AuditEventResponse::from)
        .toList();
  }
}
