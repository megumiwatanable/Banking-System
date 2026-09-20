package com.banking.dto.account;

import com.banking.model.TransactionStatus;
import java.math.BigDecimal;

public record InterbankTransferResponse(
        Long transactionId,
        String senderAccountNumber,
        String bankCode,
        String receiverAccountNumber,
        String recipientName,
        BigDecimal amount,
        TransactionStatus status
) {}
