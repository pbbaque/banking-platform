package com.pbbaque.banking.transfer.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link OutboxScheduler}. */
@ExtendWith(MockitoExtension.class)
class OutboxSchedulerTest {

  @Mock private OutboxPublisher outboxPublisher;

  @InjectMocks private OutboxScheduler outboxScheduler;

  @Test
  void shouldDelegatePendingEventPublicationToPublisher() {
    outboxScheduler.publishPendingEvents();

    verify(outboxPublisher).publishPendingEvents();
  }
}
