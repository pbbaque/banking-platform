package com.pbbaque.banking.audit.controller;

import com.pbbaque.banking.audit.dto.AuditEventResponse;
import com.pbbaque.banking.audit.service.AuditEventService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for querying audit events belonging to authenticated users. */
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
   * Retrieves an audit event by identifier.
   *
   * @param id audit event identifier
   * @param jwt authenticated user's JWT
   * @return accessible audit event
   */
  @GetMapping("/{id}")
  public ResponseEntity<AuditEventResponse> getById(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {

    return ResponseEntity.ok(auditEventService.getById(id, authenticatedActorId(jwt)));
  }

  /**
   * Retrieves audit events associated with an aggregate and the authenticated actor.
   *
   * @param aggregateType aggregate type
   * @param aggregateId aggregate identifier
   * @param jwt authenticated user's JWT
   * @return matching accessible audit events
   */
  @GetMapping(params = {"aggregateType", "aggregateId"})
  public ResponseEntity<List<AuditEventResponse>> getByAggregate(
      @RequestParam String aggregateType,
      @RequestParam String aggregateId,
      @AuthenticationPrincipal Jwt jwt) {

    return ResponseEntity.ok(
        auditEventService.getByAggregate(aggregateType, aggregateId, authenticatedActorId(jwt)));
  }

  /**
   * Retrieves audit events associated with the authenticated actor.
   *
   * <p>The supplied actor identifier must match the authenticated JWT subject.
   *
   * @param actorId requested actor identifier
   * @param jwt authenticated user's JWT
   * @return actor audit events
   */
  @GetMapping(params = "actorId")
  public ResponseEntity<List<AuditEventResponse>> getByActor(
      @RequestParam UUID actorId, @AuthenticationPrincipal Jwt jwt) {

    UUID authenticatedActorId = authenticatedActorId(jwt);

    if (!authenticatedActorId.equals(actorId)) {

      throw new AccessDeniedException("Cannot access audit events belonging to another actor");
    }

    return ResponseEntity.ok(auditEventService.getByActor(authenticatedActorId));
  }

  /**
   * Retrieves audit events of a specific type belonging to the authenticated actor.
   *
   * @param eventType event type
   * @param jwt authenticated user's JWT
   * @return matching accessible audit events
   */
  @GetMapping(params = "eventType")
  public ResponseEntity<List<AuditEventResponse>> getByEventType(
      @RequestParam String eventType, @AuthenticationPrincipal Jwt jwt) {

    return ResponseEntity.ok(
        auditEventService.getByEventType(eventType, authenticatedActorId(jwt)));
  }

  private UUID authenticatedActorId(Jwt jwt) {

    return UUID.fromString(jwt.getSubject());
  }
}
