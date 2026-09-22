package com.banking.notification.domain;

import com.banking.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor
public class Notification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(nullable = false, unique = true, length = 36)
  private String eventId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationChannel channel;

  @Column(nullable = false)
  private String subject;

  @Column(nullable = false, length = 1000)
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationStatus status;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public Notification(
      User user,
      String eventId,
      NotificationChannel channel,
      String subject,
      String message,
      NotificationStatus status) {
    this.user = user;
    this.eventId = eventId;
    this.channel = channel;
    this.subject = subject;
    this.message = message;
    this.status = status;
  }

  @PrePersist
  void createTimestamp() {
    createdAt = LocalDateTime.now();
  }
}
