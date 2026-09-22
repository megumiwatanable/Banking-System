package com.banking.transfer.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Immutable integration event; it contains identifiers, not credentials or authentication data. */
public record TransferCompletedEvent(
    UUID eventId,
    Long transactionId,
    Long customerId,
    Long sourceAccountId,
    Long destinationAccountId,
    BigDecimal amount,
    BigDecimal fee,
    String currency,
    LocalDateTime occurredAt) {}
