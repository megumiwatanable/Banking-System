package com.banking.notification.infrastructure;

import com.banking.notification.domain.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findByUserEmailOrderByCreatedAtDesc(String email);
}
