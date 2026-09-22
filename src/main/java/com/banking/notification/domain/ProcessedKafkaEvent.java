package com.banking.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processed_kafka_events")
@Getter
@NoArgsConstructor
public class ProcessedKafkaEvent {
  @Id
  @Column(length = 36)
  private String eventId;

  @Column(nullable = false, length = 100)
  private String topic;

  @Column(nullable = false, updatable = false)
  private LocalDateTime processedAt;

  public ProcessedKafkaEvent(String eventId, String topic) {
    this.eventId = eventId;
    this.topic = topic;
  }

  @PrePersist
  void createTimestamp() {
    processedAt = LocalDateTime.now();
  }
}
