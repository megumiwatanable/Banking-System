package com.banking.transfer.event;

import com.banking.transaction.domain.Transaction;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Publishes a domain event inside the transaction; the Kafka bridge waits for commit. */
@Component
public class TransferEventPublisher {
  private final ApplicationEventPublisher applicationEventPublisher;

  public TransferEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  public void publishCompleted(Transaction transaction) {
    applicationEventPublisher.publishEvent(
        new TransferCompletedEvent(
            UUID.randomUUID(),
            transaction.getId(),
            transaction.getSenderAccount().getUser().getId(),
            transaction.getSenderAccount().getId(),
            transaction.getReceiverAccount().getId(),
            transaction.getAmount(),
            transaction.getFee(),
            transaction.getCurrency(),
            transaction.getTransactionTime() == null
                ? LocalDateTime.now()
                : transaction.getTransactionTime()));
  }
}
