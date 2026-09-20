package com.banking.account.api.dto;

import com.banking.account.domain.AccountType;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
    @NotNull(message = "Account type is required") AccountType accountType) {}
