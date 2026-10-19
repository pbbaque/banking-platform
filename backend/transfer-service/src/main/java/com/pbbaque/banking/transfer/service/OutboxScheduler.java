package com.pbbaque.banking.transfer.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically triggers publication of pending transactional outbox events. */
@Component
public class OutboxScheduler {

  private final OutboxPublisher outboxPublisher;

  /**
   * Creates the outbox scheduler.
   *
   * @param outboxPublisher publisher responsible for processing pending events
   */
  public OutboxScheduler(OutboxPublisher outboxPublisher) {
    this.outboxPublisher = outboxPublisher;
  }

  /** Publishes currently available outbox events at the configured polling interval. */
  @Scheduled(fixedDelayString = "${messaging.kafka.outbox.polling-interval}")
  public void publishPendingEvents() {
    outboxPublisher.publishPendingEvents();
  }
}
