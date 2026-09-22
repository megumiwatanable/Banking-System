package com.banking.notification.application;

import com.banking.notification.api.dto.NotificationResponse;
import com.banking.notification.domain.Notification;
import com.banking.notification.infrastructure.NotificationRepository;
import com.banking.shared.security.SecurityUtils;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
  private final NotificationRepository notificationRepository;

  public NotificationService(NotificationRepository notificationRepository) {
    this.notificationRepository = notificationRepository;
  }

  @Transactional(readOnly = true)
  public List<NotificationResponse> listMine() {
    return notificationRepository
        .findByUserEmailOrderByCreatedAtDesc(SecurityUtils.getCurrentUserEmail())
        .stream()
        .map(this::toResponse)
        .toList();
  }

  private NotificationResponse toResponse(Notification notification) {
    return new NotificationResponse(
        notification.getId(),
        notification.getChannel(),
        notification.getSubject(),
        notification.getMessage(),
        notification.getStatus(),
        notification.getCreatedAt());
  }
}
