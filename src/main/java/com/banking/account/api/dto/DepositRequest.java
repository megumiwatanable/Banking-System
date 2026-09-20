package com.banking.account.api.dto;

import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record DepositRequest(
    @Positive(message = "Amount must be greate than 0") BigDecimal amount) {}
