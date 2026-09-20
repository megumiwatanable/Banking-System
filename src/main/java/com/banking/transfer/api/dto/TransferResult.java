package com.banking.transfer.api.dto;

import com.banking.transaction.domain.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResult(
    Long transactionId,
    TransactionStatus status,
    BigDecimal amount,
    BigDecimal fee,
    String currency,
    LocalDateTime transactionTime) {}
