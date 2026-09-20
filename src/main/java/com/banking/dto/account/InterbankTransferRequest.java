package com.banking.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record InterbankTransferRequest(
        @NotBlank @Pattern(regexp = "\\d{10}") String senderAccountNumber,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{2,30}") String bankCode,
        @NotBlank @Pattern(regexp = "\\d{4,30}") String receiverAccountNumber,
        @NotBlank @Size(max = 255) String recipientName,
        @NotNull @Positive BigDecimal amount
) {}
