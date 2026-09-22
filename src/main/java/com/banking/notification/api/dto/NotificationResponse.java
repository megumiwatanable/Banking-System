package com.banking.notification.api.dto;

import com.banking.notification.domain.NotificationChannel;
import com.banking.notification.domain.NotificationStatus;
import java.time.LocalDateTime;

public record NotificationResponse(
    Long id,
    NotificationChannel channel,
    String subject,
    String message,
    NotificationStatus status,
    LocalDateTime createdAt) {}
