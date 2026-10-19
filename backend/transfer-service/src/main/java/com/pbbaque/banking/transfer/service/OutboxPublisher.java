package com.pbbaque.banking.transfer.service;

import com.pbbaque.banking.transfer.config.AuditEventsTopicProperties;
import com.pbbaque.banking.transfer.config.OutboxPublisherProperties;
import com.pbbaque.banking.transfer.domain.OutboxEvent;
import com.pbbaque.banking.transfer.domain.OutboxEventStatus;
import com.pbbaque.banking.transfer.repository.OutboxEventRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/** Publishes pending transactional outbox events to Kafka. */
@Service
public class OutboxPublisher {

  private final OutboxEventRepository outboxEventRepository;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification =
          "KafkaTemplate is a Spring-managed dependency intentionally retained by the service.")
  private final KafkaTemplate<String, String> kafkaTemplate;

  private final AuditEventsTopicProperties topicProperties;
  private final OutboxPublisherProperties publisherProperties;

  /**
   * Creates the outbox publisher.
   *
   * @param outboxEventRepository outbox event repository
   * @param kafkaTemplate Kafka producer
   * @param topicProperties audit events topic configuration
   * @param publisherProperties outbox publisher configuration
   */
  public OutboxPublisher(
      OutboxEventRepository outboxEventRepository,
      KafkaTemplate<String, String> kafkaTemplate,
      AuditEventsTopicProperties topicProperties,
      OutboxPublisherProperties publisherProperties) {
    this.outboxEventRepository = outboxEventRepository;
    this.kafkaTemplate = kafkaTemplate;
    this.topicProperties = topicProperties;
    this.publisherProperties = publisherProperties;
  }

  /** Publishes the currently available pending outbox events. */
  public void publishPendingEvents() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    List<OutboxEvent> events =
        outboxEventRepository.findByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            OutboxEventStatus.PENDING, now, PageRequest.of(0, publisherProperties.batchSize()));

    for (OutboxEvent event : events) {
      publish(event);
    }
  }

  private void publish(OutboxEvent event) {
    try {
      kafkaTemplate
          .send(topicProperties.name(), event.getAggregateId().toString(), event.getPayload())
          .get(publisherProperties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);

      event.markPublished(OffsetDateTime.now(ZoneOffset.UTC));
      outboxEventRepository.save(event);
    } catch (Exception exception) {
      event.registerPublicationFailure(
          publicationError(exception),
          OffsetDateTime.now(ZoneOffset.UTC).plus(publisherProperties.retryDelay()));

      outboxEventRepository.save(event);
    }
  }

  private String publicationError(Exception exception) {
    String message = exception.getMessage();

    if (message == null || message.isBlank()) {
      return exception.getClass().getSimpleName();
    }

    return exception.getClass().getSimpleName() + ": " + message;
  }
}
