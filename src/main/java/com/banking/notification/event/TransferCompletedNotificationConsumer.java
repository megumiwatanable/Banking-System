package com.banking.notification.event;

import com.banking.notification.domain.Notification;
import com.banking.notification.domain.NotificationChannel;
import com.banking.notification.domain.NotificationStatus;
import com.banking.notification.domain.ProcessedKafkaEvent;
import com.banking.notification.infrastructure.NotificationRepository;
import com.banking.notification.infrastructure.ProcessedKafkaEventRepository;
import com.banking.transfer.event.TransferCompletedEvent;
import com.banking.user.domain.User;
import com.banking.user.infrastructure.UserRepository;
import java.math.RoundingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransferCompletedNotificationConsumer {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(TransferCompletedNotificationConsumer.class);

  private final ProcessedKafkaEventRepository processedEventRepository;
  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final String topic;

  public TransferCompletedNotificationConsumer(
      ProcessedKafkaEventRepository processedEventRepository,
      NotificationRepository notificationRepository,
      UserRepository userRepository,
      @Value("${banking.kafka.topics.transfer-completed}") String topic) {
    this.processedEventRepository = processedEventRepository;
    this.notificationRepository = notificationRepository;
    this.userRepository = userRepository;
    this.topic = topic;
  }

  @KafkaListener(topics = "${banking.kafka.topics.transfer-completed}")
  @Transactional
  public void consume(TransferCompletedEvent event) {
    String eventId = event.eventId().toString();
    if (processedEventRepository.existsById(eventId)) {
      LOGGER.info("Ignoring duplicate Kafka event {}", eventId);
      return;
    }

    User customer =
        userRepository
            .findById(event.customerId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Customer no longer exists for event " + eventId));
    String amount = event.amount().setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    Notification notification =
        new Notification(
            customer,
            eventId,
            NotificationChannel.IN_APP,
            "Chuyển tiền thành công",
            "Giao dịch #"
                + event.transactionId()
                + " đã chuyển "
                + amount
                + " "
                + event.currency()
                + ".",
            NotificationStatus.SENT);

    // Both rows commit together. A Kafka retry sees the event marker and becomes a no-op.
    notificationRepository.save(notification);
    processedEventRepository.save(new ProcessedKafkaEvent(eventId, topic));
  }
}
