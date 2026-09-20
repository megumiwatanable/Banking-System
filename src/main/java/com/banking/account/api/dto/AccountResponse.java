package com.banking.account.api.dto;

import com.banking.account.domain.AccountType;
import com.banking.account.domain.AccountStatus;
import java.math.BigDecimal;

public record AccountResponse(
    Long id, String accountNumber, AccountType accountType, BigDecimal balance,
    BigDecimal availableBalance, BigDecimal holdAmount, String currency, AccountStatus status) {
  public AccountResponse(Long id, String accountNumber, AccountType accountType, BigDecimal balance) {
    this(id, accountNumber, accountType, balance, balance, BigDecimal.ZERO, "VND", AccountStatus.ACTIVE);
  }
}
