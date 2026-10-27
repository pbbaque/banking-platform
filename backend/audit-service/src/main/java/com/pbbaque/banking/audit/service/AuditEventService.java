package com.pbbaque.banking.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbbaque.banking.audit.domain.AuditEvent;
import com.pbbaque.banking.audit.dto.AuditEventMessage;
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
  private final ObjectMapper objectMapper;

  /**
   * Creates a new audit event service.
   *
   * @param auditEventRepository audit event repository
   * @param objectMapper JSON serializer
   */
  public AuditEventService(AuditEventRepository auditEventRepository, ObjectMapper objectMapper) {

    this.auditEventRepository = auditEventRepository;

    this.objectMapper = objectMapper.copy();
  }

  /**
   * Creates and persists a new audit event.
   *
   * <p>This method is not exposed through the public HTTP API.
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
   * Persists an audit event received from the message broker.
   *
   * <p>If an event with the same identifier already exists, the message is treated as already
   * processed and no duplicate audit event is created.
   *
   * @param message consumed audit event message
   * @return {@code true} when a new event was persisted, {@code false} when it already existed
   */
  @Transactional
  public boolean createFromMessage(AuditEventMessage message) {

    if (auditEventRepository.existsById(message.eventId())) {
      return false;
    }

    AuditEvent auditEvent =
        AuditEvent.create(
            message.eventId(),
            message.eventType(),
            message.aggregateType(),
            message.aggregateId(),
            message.actorId(),
            message.outcome(),
            message.description(),
            serializeMetadata(message),
            message.occurredAt());

    auditEventRepository.save(auditEvent);

    return true;
  }

  /**
   * Finds an audit event belonging to a specific actor.
   *
   * <p>An event owned by another actor is deliberately treated as not found so its existence is not
   * disclosed.
   *
   * @param id audit event identifier
   * @param actorId authenticated actor identifier
   * @return audit event
   * @throws AuditEventNotFoundException when no accessible event exists
   */
  @Transactional(readOnly = true)
  public AuditEventResponse getById(UUID id, UUID actorId) {

    AuditEvent auditEvent =
        auditEventRepository
            .findByIdAndActorId(id, actorId)
            .orElseThrow(() -> new AuditEventNotFoundException(id));

    return AuditEventResponse.from(auditEvent);
  }

  /**
   * Finds audit events for a specific aggregate and actor.
   *
   * @param aggregateType aggregate type
   * @param aggregateId aggregate identifier
   * @param actorId authenticated actor identifier
   * @return matching accessible audit events
   */
  @Transactional(readOnly = true)
  public List<AuditEventResponse> getByAggregate(
      String aggregateType, String aggregateId, UUID actorId) {

    return auditEventRepository
        .findAllByAggregateTypeAndAggregateIdAndActorId(aggregateType, aggregateId, actorId)
        .stream()
        .map(AuditEventResponse::from)
        .toList();
  }

  /**
   * Finds audit events created by a specific actor.
   *
   * @param actorId authenticated actor identifier
   * @return matching audit events
   */
  @Transactional(readOnly = true)
  public List<AuditEventResponse> getByActor(UUID actorId) {

    return auditEventRepository.findAllByActorId(actorId).stream()
        .map(AuditEventResponse::from)
        .toList();
  }

  /**
   * Finds audit events of a specific type belonging to an actor.
   *
   * @param eventType event type
   * @param actorId authenticated actor identifier
   * @return matching accessible audit events
   */
  @Transactional(readOnly = true)
  public List<AuditEventResponse> getByEventType(String eventType, UUID actorId) {

    return auditEventRepository.findAllByEventTypeAndActorId(eventType, actorId).stream()
        .map(AuditEventResponse::from)
        .toList();
  }

  private String serializeMetadata(AuditEventMessage message) {

    if (message.metadata().isEmpty()) {
      return null;
    }

    try {
      return objectMapper.writeValueAsString(message.metadata());

    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize audit event metadata", exception);
    }
  }
}
