package com.banking.account.application;

import com.banking.account.api.dto.AccountResponse;
import com.banking.account.api.dto.CreateAccountRequest;
import com.banking.account.api.dto.DepositRequest;
import com.banking.account.api.dto.InterbankTransferRequest;
import com.banking.account.api.dto.InterbankTransferResponse;
import com.banking.account.api.dto.TransferRequest;
import com.banking.account.api.dto.WithdrawRequest;
import com.banking.account.domain.Account;
import com.banking.account.infrastructure.AccountRepository;
import com.banking.shared.error.*;
import com.banking.shared.security.SecurityUtils;
import com.banking.transaction.domain.Transaction;
import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.domain.TransactionType;
import com.banking.transaction.infrastructure.TransactionRepository;
import com.banking.user.domain.User;
import com.banking.user.infrastructure.UserRepository;
import java.security.SecureRandom;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountServiceImpl implements AccountService {
  private static final String ACCOUNT_CACHE = "accountCacheV2";

  private final AccountRepository accountRepository;
  private final TransactionRepository transactionRepository;
  private final UserRepository userRepository;
  private final SecureRandom random = new SecureRandom();

  public AccountServiceImpl(
      AccountRepository accountRepository,
      TransactionRepository transactionRepository,
      UserRepository userRepository) {
    this.accountRepository = accountRepository;
    this.transactionRepository = transactionRepository;
    this.userRepository = userRepository;
  }

  @Override
  public List<AccountResponse> getMyAccounts() {
    return accountRepository
        .findByUserEmailOrderByIdDesc(SecurityUtils.getCurrentUserEmail())
        .stream()
        .map(this::toAccountResponse)
        .toList();
  }

  @Override
  @Transactional
  @CacheEvict(value = ACCOUNT_CACHE, allEntries = true)
  public AccountResponse create(CreateAccountRequest request) {
    User currentUser = getCurrentUserEntity();

    Account account =
        new Account(generateUniqueAccountNumber(), request.accountType(), currentUser);
    Account saved = accountRepository.save(account);

    return toAccountResponse(saved);
  }

  @Override
  @Cacheable(
      value = ACCOUNT_CACHE,
      key = "#accountId + ':' + T(com.banking.shared.security.SecurityUtils).getCurrentUserEmail()")
  public AccountResponse getAccountById(Long accountId) {
    Account account = getOwnedAccount(accountId);
    return toAccountResponse(account);
  }

  @Override
  @Transactional
  @CacheEvict(
      value = {ACCOUNT_CACHE, "allTransactionsV2", "transactionByIdV2", "transactionsByAccountV2"},
      allEntries = true)
  public AccountResponse deposit(Long accountId, DepositRequest request) {
    User currentUser = getCurrentUserEntity();
    Account account = getOwnedAccountForUpdate(accountId, currentUser);

    account.setBalance(account.getBalance().add(request.amount()));
    accountRepository.save(account);

    Transaction transaction =
        new Transaction(TransactionType.DEPOSIT, request.amount(), null, account);
    transaction.setStatus(TransactionStatus.SUCCESS);
    transactionRepository.save(transaction);

    return toAccountResponse(account);
  }

  @Override
  @Transactional
  @CacheEvict(
      value = {ACCOUNT_CACHE, "allTransactionsV2", "transactionByIdV2", "transactionsByAccountV2"},
      allEntries = true)
  public AccountResponse withdraw(Long accountId, WithdrawRequest request) {
    User currentUser = getCurrentUserEntity();
    Account account = getOwnedAccountForUpdate(accountId, currentUser);

    if (account.getBalance().compareTo(request.amount()) < 0) {
      throw new InsufficientBalanceException("Insufficient balance for this withdrawal");
    }

    account.setBalance(account.getBalance().subtract(request.amount()));
    accountRepository.save(account);

    Transaction transaction =
        new Transaction(TransactionType.WITHDRAW, request.amount(), account, null);
    transaction.setStatus(TransactionStatus.SUCCESS);
    transactionRepository.save(transaction);

    return toAccountResponse(account);
  }

  @Override
  @Transactional
  @CacheEvict(
      value = {ACCOUNT_CACHE, "allTransactionsV2", "transactionByIdV2", "transactionsByAccountV2"},
      allEntries = true)
  public AccountResponse transfer(TransferRequest request) {
    if (request.senderAccountNumber().equals(request.receiverAccountNumber())) {
      throw new InvalidAccountOperationException("Cannot transfer to the same account");
    }

    User currentUser = getCurrentUserEntity();

    Account senderReference =
        accountRepository
            .findByAccountNumber(request.senderAccountNumber())
            .orElseThrow(() -> new ResourceNotFoundException("Sender account not found"));
    if (!senderReference.getUser().getId().equals(currentUser.getId())) {
      throw new UnauthorizedAccessException("You do not have access to this account");
    }

    Account receiverReference =
        accountRepository
            .findByAccountNumber(request.receiverAccountNumber())
            .orElseThrow(() -> new ResourceNotFoundException("Receiver account not found"));

    Long firstId = Math.min(senderReference.getId(), receiverReference.getId());
    Long secondId = Math.max(senderReference.getId(), receiverReference.getId());

    Account first = lockAccountOrThrow(firstId);
    Account second = lockAccountOrThrow(secondId);

    Account sender = senderReference.getId().equals(firstId) ? first : second;
    Account receiver = senderReference.getId().equals(firstId) ? second : first;

    if (!sender.getUser().getId().equals(currentUser.getId())) {
      throw new UnauthorizedAccessException("You do not have access to this account");
    }

    if (sender.getBalance().compareTo(request.amount()) < 0) {
      throw new InsufficientBalanceException("Insufficient balance for this transfer");
    }

    sender.setBalance(sender.getBalance().subtract(request.amount()));
    receiver.setBalance(receiver.getBalance().add(request.amount()));

    accountRepository.save(sender);
    accountRepository.save(receiver);

    Transaction transaction =
        new Transaction(TransactionType.TRANSFER, request.amount(), sender, receiver);
    transaction.setStatus(TransactionStatus.SUCCESS);
    transactionRepository.save(transaction);

    return toAccountResponse(sender);
  }

  @Override
  @Transactional
  @CacheEvict(
      value = {ACCOUNT_CACHE, "allTransactionsV2", "transactionByIdV2", "transactionsByAccountV2"},
      allEntries = true)
  public InterbankTransferResponse requestInterbankTransfer(InterbankTransferRequest request) {
    User currentUser = getCurrentUserEntity();
    Account sender =
        accountRepository
            .findByAccountNumber(request.senderAccountNumber())
            .orElseThrow(() -> new ResourceNotFoundException("Sender account not found"));
    if (!sender.getUser().getId().equals(currentUser.getId())) {
      throw new UnauthorizedAccessException("You do not have access to this account");
    }

    Transaction transaction =
        new Transaction(TransactionType.INTERBANK_TRANSFER, request.amount(), sender, null);
    transaction.setExternalBankCode(request.bankCode().trim().toUpperCase());
    transaction.setExternalAccountNumber(request.receiverAccountNumber().trim());
    transaction.setExternalRecipientName(request.recipientName().trim());
    Transaction saved = transactionRepository.save(transaction);

    return new InterbankTransferResponse(
        saved.getId(),
        sender.getAccountNumber(),
        saved.getExternalBankCode(),
        saved.getExternalAccountNumber(),
        saved.getExternalRecipientName(),
        saved.getAmount(),
        saved.getStatus());
  }

  private Account getOwnedAccount(Long accountId) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Account not found with id: " + accountId));

    User currentUser = getCurrentUserEntity();
    if (!account.getUser().getId().equals(currentUser.getId())) {
      throw new UnauthorizedAccessException("You do not have access to this account");
    }

    return account;
  }

  private Account getOwnedAccountForUpdate(Long accountId, User currentUser) {
    Account account = lockAccountOrThrow(accountId);

    if (!account.getUser().getId().equals(currentUser.getId())) {
      throw new UnauthorizedAccessException("You do not have access to this account");
    }

    return account;
  }

  private Account lockAccountOrThrow(Long accountId) {
    try {
      return accountRepository
          .findByIdForUpdate(accountId)
          .orElseThrow(
              () -> new ResourceNotFoundException("Account not found with id: " + accountId));
    } catch (PessimisticLockingFailureException e) {
      throw new AccountLockTimeoutException(
          "Account " + accountId + " is currently locked by another operation");
    }
  }

  private User getCurrentUserEntity() {
    String email = SecurityUtils.getCurrentUserEmail();
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
  }

  private String generateUniqueAccountNumber() {
    String accountNumber;
    do {
      long candidate = 1_000_000_000L + (long) (random.nextDouble() * 9_000_000_000L);
      accountNumber = String.valueOf(candidate);
    } while (accountRepository.existsByAccountNumber(accountNumber));

    return accountNumber;
  }

  private AccountResponse toAccountResponse(Account account) {
    return new AccountResponse(
        account.getId(),
        account.getAccountNumber(),
        account.getAccountType(),
        account.getBalance());
  }
}
