package com.banking.transfer.application;

import com.banking.account.domain.Account;
import com.banking.account.domain.AccountStatus;
import com.banking.shared.error.InsufficientBalanceException;
import com.banking.shared.error.InvalidAccountOperationException;
import com.banking.shared.error.InvalidTransactionException;
import com.banking.shared.error.UnauthorizedAccessException;
import com.banking.transaction.infrastructure.TransactionRepository;
import com.banking.transfer.api.dto.InternalTransferRequest;
import com.banking.transfer.infrastructure.TransactionLimitRepository;
import com.banking.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Centralizes transfer rules so the transaction coordinator remains easy to audit. */
@Component
public class TransferPolicy {
  private final TransactionLimitRepository limitRepository;
  private final TransactionRepository transactionRepository;
  private final BigDecimal flatFee;
  private final BigDecimal defaultPerTransactionLimit;
  private final BigDecimal defaultDailyLimit;

  public TransferPolicy(
      TransactionLimitRepository limitRepository,
      TransactionRepository transactionRepository,
      @Value("${banking.transfer.flat-fee:0.00}") BigDecimal flatFee,
      @Value("${banking.transfer.default-per-transaction-limit:100000000.00}")
          BigDecimal defaultPerTransactionLimit,
      @Value("${banking.transfer.default-daily-limit:500000000.00}")
          BigDecimal defaultDailyLimit) {
    this.limitRepository = limitRepository;
    this.transactionRepository = transactionRepository;
    this.flatFee = flatFee;
    this.defaultPerTransactionLimit = defaultPerTransactionLimit;
    this.defaultDailyLimit = defaultDailyLimit;
  }

  public void validate(
      Account source, Account destination, InternalTransferRequest request, User customer) {
    if (!source.getUser().getId().equals(customer.getId())) {
      throw new UnauthorizedAccessException("Source account does not belong to you");
    }
    if (source.getStatus() != AccountStatus.ACTIVE
        || destination.getStatus() != AccountStatus.ACTIVE) {
      throw new InvalidAccountOperationException("Both accounts must be active");
    }
    if (!source.getCurrency().equals(request.currency())
        || !destination.getCurrency().equals(request.currency())) {
      throw new InvalidTransactionException("Currency mismatch");
    }

    verifyLimits(source, customer, request);

    // Held money remains booked but must never be considered spendable.
    BigDecimal requiredBalance = request.amount().add(flatFee);
    if (source.getAvailableBalance().compareTo(requiredBalance) < 0) {
      throw new InsufficientBalanceException(
          "Available balance is insufficient for amount and fee");
    }
  }

  public BigDecimal fee() {
    return flatFee;
  }

  private void verifyLimits(Account source, User customer, InternalTransferRequest request) {
    BigDecimal perTransactionLimit = defaultPerTransactionLimit;
    BigDecimal dailyLimit = defaultDailyLimit;
    var configuredLimit =
        limitRepository.findByUserIdAndCurrency(customer.getId(), request.currency());
    if (configuredLimit.isPresent()) {
      perTransactionLimit = configuredLimit.get().getPerTransactionLimit();
      dailyLimit = configuredLimit.get().getDailyLimit();
    }

    if (request.amount().compareTo(perTransactionLimit) > 0) {
      throw new InvalidTransactionException("Per-transaction limit exceeded");
    }

    LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
    BigDecimal transferredToday =
        transactionRepository.sumSuccessfulOutgoingSince(source.getId(), startOfDay);
    if (transferredToday.add(request.amount()).compareTo(dailyLimit) > 0) {
      throw new InvalidTransactionException("Daily limit exceeded");
    }
  }
}
