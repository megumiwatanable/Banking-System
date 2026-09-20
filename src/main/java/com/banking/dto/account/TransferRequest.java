package com.banking.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest (
        @NotBlank(message = "Sender account number is required")
        @Pattern(regexp = "\\d{10}", message = "Sender account number must have 10 digits")
        String senderAccountNumber,

        @NotBlank(message = "Receiver account number is required")
        @Pattern(regexp = "\\d{10}", message = "Receiver account number must have 10 digits")
        String receiverAccountNumber,

        @NotNull @Positive(message = "Amount must be greater than 0")
        BigDecimal amount
){}
