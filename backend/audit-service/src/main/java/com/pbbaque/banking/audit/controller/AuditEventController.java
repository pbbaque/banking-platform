package com.pbbaque.banking.audit.controller;

import com.pbbaque.banking.audit.dto.AuditEventResponse;
import com.pbbaque.banking.audit.dto.CreateAuditEventRequest;
import com.pbbaque.banking.audit.service.AuditEventService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for creating and querying audit events. */
@RestController
@RequestMapping("/api/audit-events")
public class AuditEventController {

  private final AuditEventService auditEventService;

  /**
   * Creates a new audit event controller.
   *
   * @param auditEventService audit event service
   */
  public AuditEventController(AuditEventService auditEventService) {
    this.auditEventService = auditEventService;
  }

  /**
   * Creates a new audit event.
   *
   * @param request audit event creation request
   * @return created audit event
   */
  @PostMapping
  public ResponseEntity<AuditEventResponse> create(
      @Valid @RequestBody CreateAuditEventRequest request) {

    AuditEventResponse response = auditEventService.create(request);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Retrieves an audit event by identifier.
   *
   * @param id audit event identifier
   * @return audit event
   */
  @GetMapping("/{id}")
  public ResponseEntity<AuditEventResponse> getById(@PathVariable UUID id) {
    return ResponseEntity.ok(auditEventService.getById(id));
  }

  /**
   * Retrieves audit events associated with an aggregate.
   *
   * @param aggregateType aggregate type
   * @param aggregateId aggregate identifier
   * @return matching audit events
   */
  @GetMapping(params = {"aggregateType", "aggregateId"})
  public ResponseEntity<List<AuditEventResponse>> getByAggregate(
      @RequestParam String aggregateType, @RequestParam String aggregateId) {

    return ResponseEntity.ok(auditEventService.getByAggregate(aggregateType, aggregateId));
  }

  /**
   * Retrieves audit events associated with an actor.
   *
   * @param actorId actor identifier
   * @return matching audit events
   */
  @GetMapping(params = "actorId")
  public ResponseEntity<List<AuditEventResponse>> getByActor(@RequestParam UUID actorId) {

    return ResponseEntity.ok(auditEventService.getByActor(actorId));
  }

  /**
   * Retrieves audit events of a specific type.
   *
   * @param eventType event type
   * @return matching audit events
   */
  @GetMapping(params = "eventType")
  public ResponseEntity<List<AuditEventResponse>> getByEventType(@RequestParam String eventType) {

    return ResponseEntity.ok(auditEventService.getByEventType(eventType));
  }
}
