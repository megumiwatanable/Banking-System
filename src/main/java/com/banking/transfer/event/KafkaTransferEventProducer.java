package com.banking.transfer.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class KafkaTransferEventProducer {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(KafkaTransferEventProducer.class);

  private final KafkaTemplate<String, TransferCompletedEvent> kafkaTemplate;
  private final String transferCompletedTopic;

  public KafkaTransferEventProducer(
      KafkaTemplate<String, TransferCompletedEvent> kafkaTemplate,
      @Value("${banking.kafka.topics.transfer-completed}") String transferCompletedTopic) {
    this.kafkaTemplate = kafkaTemplate;
    this.transferCompletedTopic = transferCompletedTopic;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void send(TransferCompletedEvent event) {
    // Kafka availability must not decide whether an already committed transfer succeeds.
    kafkaTemplate
        .send(transferCompletedTopic, event.customerId().toString(), event)
        .whenComplete(
            (result, exception) -> {
              if (exception != null) {
                LOGGER.error(
                    "Could not publish transfer event {} for transaction {}",
                    event.eventId(),
                    event.transactionId(),
                    exception);
              } else {
                LOGGER.info(
                    "Published transfer event {} to {} partition {}",
                    event.eventId(),
                    transferCompletedTopic,
                    result.getRecordMetadata().partition());
              }
            });
  }
}
