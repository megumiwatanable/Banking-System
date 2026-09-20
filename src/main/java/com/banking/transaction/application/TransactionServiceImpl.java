package com.banking.transaction.application;

import com.banking.account.domain.Account;
import com.banking.account.infrastructure.AccountRepository;
import com.banking.shared.error.ResourceNotFoundException;
import com.banking.shared.error.UnauthorizedAccessException;
import com.banking.shared.security.SecurityUtils;
import com.banking.transaction.api.dto.TransactionResponse;
import com.banking.transaction.domain.Transaction;
import com.banking.transaction.infrastructure.TransactionRepository;
import com.banking.user.domain.User;
import com.banking.user.infrastructure.UserRepository;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class TransactionServiceImpl implements TransactionService {

  private final TransactionRepository transactionRepository;
  private final AccountRepository accountRepository;
  private final UserRepository userRepository;

  public TransactionServiceImpl(
      TransactionRepository transactionRepository,
      AccountRepository accountRepository,
      UserRepository userRepository) {
    this.transactionRepository = transactionRepository;
    this.accountRepository = accountRepository;
    this.userRepository = userRepository;
  }

  @Override
  @Cacheable(
      value = "allTransactionsV2",
      key = "T(com.banking.shared.security.SecurityUtils).getCurrentUserEmail()")
  public List<TransactionResponse> getAllTransactions() {
    String email = SecurityUtils.getCurrentUserEmail();

    return transactionRepository
        .findBySenderAccountUserEmailOrReceiverAccountUserEmail(email, email)
        .stream()
        .map(this::toTransactionResponse)
        .toList();
  }

  @Override
  @Cacheable(
      value = "transactionByIdV2",
      key =
          "#transactionId + ':' +"
              + " T(com.banking.shared.security.SecurityUtils).getCurrentUserEmail()")
  public TransactionResponse getTransactionById(Long transactionId) {
    Transaction transaction =
        transactionRepository
            .findById(transactionId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Transaction not found with id: " + transactionId));

    User currentUser = getCurrentUserEntity();
    ensureTransactionOwnership(transaction, currentUser);

    return toTransactionResponse(transaction);
  }

  @Override
  @Cacheable(
      value = "transactionsByAccountV2",
      key = "#accountId + ':' + T(com.banking.shared.security.SecurityUtils).getCurrentUserEmail()")
  public List<TransactionResponse> getTransactionForAcc(Long accountId) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Account not found with id: " + accountId));

    User currentUser = getCurrentUserEntity();
    if (!account.getUser().getId().equals(currentUser.getId())) {
      throw new UnauthorizedAccessException("You do not have access to this account");
    }

    return transactionRepository
        .findBySenderAccount_IdOrReceiverAccount_IdOrderByTransactionTimeDesc(accountId, accountId)
        .stream()
        .map(this::toTransactionResponse)
        .toList();
  }

  private User getCurrentUserEntity() {
    String email = SecurityUtils.getCurrentUserEmail();
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
  }

  private TransactionResponse toTransactionResponse(Transaction transaction) {
    return new TransactionResponse(
        transaction.getId(),
        transaction.getTransactionType(),
        transaction.getAmount(),
        accountNumberOf(transaction.getSenderAccount()),
        receiverAccountNumberOf(transaction),
        transaction.getTransactionTime(),
        transaction.getStatus());
  }

  private void ensureTransactionOwnership(Transaction transaction, User user) {
    boolean isSender = belongsTo(transaction.getSenderAccount(), user);
    boolean isReceiver = belongsTo(transaction.getReceiverAccount(), user);

    if (!isSender && !isReceiver) {
      throw new UnauthorizedAccessException("You do not have access to this transaction");
    }
  }

  private boolean belongsTo(Account account, User user) {
    return account != null && account.getUser().getId().equals(user.getId());
  }

  private String receiverAccountNumberOf(Transaction transaction) {
    String internalAccountNumber = accountNumberOf(transaction.getReceiverAccount());
    return internalAccountNumber != null
        ? internalAccountNumber
        : transaction.getExternalAccountNumber();
  }

  private String accountNumberOf(Account account) {
    return account == null ? null : account.getAccountNumber();
  }
}
