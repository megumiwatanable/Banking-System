package com.banking.account.api.dto;

import com.banking.transaction.domain.TransactionStatus;
import java.math.BigDecimal;

public record InterbankTransferResponse(
    Long transactionId,
    String senderAccountNumber,
    String bankCode,
    String receiverAccountNumber,
    String recipientName,
    BigDecimal amount,
    TransactionStatus status) {}
