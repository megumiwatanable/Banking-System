package com.banking.notification.infrastructure;

import com.banking.notification.domain.ProcessedKafkaEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedKafkaEventRepository
    extends JpaRepository<ProcessedKafkaEvent, String> {}
