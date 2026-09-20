package com.banking.account.api.dto;

import com.banking.account.domain.AccountType;
import java.math.BigDecimal;

public record AccountResponse(
    Long id, String accountNumber, AccountType accountType, BigDecimal balance) {}
