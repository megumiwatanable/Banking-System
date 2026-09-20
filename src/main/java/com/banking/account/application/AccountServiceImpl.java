package com.banking.account.application;

import com.banking.account.api.dto.AccountResponse;
import com.banking.account.api.dto.CreateAccountRequest;
import com.banking.account.api.dto.DepositRequest;
import com.banking.account.api.dto.InterbankTransferRequest;
import com.banking.account.api.dto.InterbankTransferResponse;
import com.banking.account.api.dto.TransferRequest;
import com.banking.account.api.dto.WithdrawRequest;
import com.banking.account.domain.Account;
import com.banking.account.domain.AccountStatus;
import com.banking.account.infrastructure.AccountRepository;
import com.banking.ledger.domain.LedgerEntry;
import com.banking.ledger.domain.LedgerEntryType;
import com.banking.ledger.infrastructure.LedgerEntryRepository;
import com.banking.shared.error.AccountLockTimeoutException;
import com.banking.shared.error.InsufficientBalanceException;
import com.banking.shared.error.InvalidAccountOperationException;
import com.banking.shared.error.ResourceNotFoundException;
import com.banking.shared.error.UnauthorizedAccessException;
import com.banking.shared.security.SecurityUtils;
import com.banking.transaction.domain.Transaction;
import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.domain.TransactionType;
import com.banking.transaction.infrastructure.TransactionRepository;
import com.banking.user.domain.User;
import com.banking.user.infrastructure.UserRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountServiceImpl implements AccountService {
  private static final String ACCOUNT_CACHE = "accountCacheV2";
  private static final String ALL_TRANSACTIONS_CACHE = "allTransactionsV2";
  private static final String TRANSACTION_BY_ID_CACHE = "transactionByIdV2";
  private static final String TRANSACTIONS_BY_ACCOUNT_CACHE = "transactionsByAccountV2";

  private final AccountRepository accountRepository;
  private final TransactionRepository transactionRepository;
  private final UserRepository userRepository;
  private final LedgerEntryRepository ledgerEntryRepository;
  private final SecureRandom secureRandom = new SecureRandom();

  public AccountServiceImpl(
      AccountRepository accountRepository,
      TransactionRepository transactionRepository,
      UserRepository userRepository) {
    this(accountRepository, transactionRepository, userRepository, null);
  }

  @Autowired
  public AccountServiceImpl(
      AccountRepository accountRepository,
      TransactionRepository transactionRepository,
      UserRepository userRepository,
      LedgerEntryRepository ledgerEntryRepository) {
    this.accountRepository = accountRepository;
    this.transactionRepository = transactionRepository;
    this.userRepository = userRepository;
    this.ledgerEntryRepository = ledgerEntryRepository;
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
      value = {
        ACCOUNT_CACHE,
        ALL_TRANSACTIONS_CACHE,
        TRANSACTION_BY_ID_CACHE,
        TRANSACTIONS_BY_ACCOUNT_CACHE
      },
      allEntries = true)
  public AccountResponse deposit(Long accountId, DepositRequest request) {
    User currentUser = getCurrentUserEntity();
    Account account = getOwnedAccountForUpdate(accountId, currentUser);

    ensureActive(account);
    BigDecimal before = account.getBalance();
    account.setBalance(before.add(request.amount()));
    accountRepository.save(account);

    Transaction transaction = saveSuccessfulTransaction(TransactionType.DEPOSIT, request.amount(), null, account);
    saveLedger(transaction, account, LedgerEntryType.CREDIT, request.amount(), before, account.getBalance());

    return toAccountResponse(account);
  }

  @Override
  @Transactional
  @CacheEvict(
      value = {
        ACCOUNT_CACHE,
        ALL_TRANSACTIONS_CACHE,
        TRANSACTION_BY_ID_CACHE,
        TRANSACTIONS_BY_ACCOUNT_CACHE
      },
      allEntries = true)
  public AccountResponse withdraw(Long accountId, WithdrawRequest request) {
    User currentUser = getCurrentUserEntity();
    Account account = getOwnedAccountForUpdate(accountId, currentUser);
    ensureActive(account);
    ensureSufficientBalance(account, request.amount(), "withdrawal");
    BigDecimal before = account.getBalance();
    account.setBalance(before.subtract(request.amount()));
    accountRepository.save(account);

    Transaction transaction = saveSuccessfulTransaction(TransactionType.WITHDRAW, request.amount(), account, null);
    saveLedger(transaction, account, LedgerEntryType.DEBIT, request.amount(), before, account.getBalance());

    return toAccountResponse(account);
  }

  @Override
  @Transactional
  @CacheEvict(
      value = {
        ACCOUNT_CACHE,
        ALL_TRANSACTIONS_CACHE,
        TRANSACTION_BY_ID_CACHE,
        TRANSACTIONS_BY_ACCOUNT_CACHE
      },
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
    ensureAccountOwnership(senderReference, currentUser);

    Account receiverReference =
        accountRepository
            .findByAccountNumber(request.receiverAccountNumber())
            .orElseThrow(() -> new ResourceNotFoundException("Receiver account not found"));

    long firstId = Math.min(senderReference.getId(), receiverReference.getId());
    long secondId = Math.max(senderReference.getId(), receiverReference.getId());

    // Every transfer locks accounts in ID order so opposite-direction transfers cannot deadlock.
    Account first = lockAccountOrThrow(firstId);
    Account second = lockAccountOrThrow(secondId);

    Account sender = senderReference.getId().equals(firstId) ? first : second;
    Account receiver = senderReference.getId().equals(firstId) ? second : first;

    ensureAccountOwnership(sender, currentUser);
    ensureActive(sender);
    ensureActive(receiver);
    if (!sender.getCurrency().equals(receiver.getCurrency())) {
      throw new InvalidAccountOperationException("Account currencies do not match");
    }
    ensureSufficientBalance(sender, request.amount(), "transfer");

    BigDecimal senderBefore = sender.getBalance();
    BigDecimal receiverBefore = receiver.getBalance();
    sender.setBalance(senderBefore.subtract(request.amount()));
    receiver.setBalance(receiverBefore.add(request.amount()));

    accountRepository.save(sender);
    accountRepository.save(receiver);

    Transaction transaction = saveSuccessfulTransaction(TransactionType.INTERNAL_TRANSFER, request.amount(), sender, receiver);
    saveLedger(transaction, sender, LedgerEntryType.DEBIT, request.amount(), senderBefore, sender.getBalance());
    saveLedger(transaction, receiver, LedgerEntryType.CREDIT, request.amount(), receiverBefore, receiver.getBalance());

    return toAccountResponse(sender);
  }

  @Override
  @Transactional
  @CacheEvict(
      value = {
        ACCOUNT_CACHE,
        ALL_TRANSACTIONS_CACHE,
        TRANSACTION_BY_ID_CACHE,
        TRANSACTIONS_BY_ACCOUNT_CACHE
      },
      allEntries = true)
  public InterbankTransferResponse requestInterbankTransfer(InterbankTransferRequest request) {
    User currentUser = getCurrentUserEntity();
    Account sender =
        accountRepository
            .findByAccountNumber(request.senderAccountNumber())
            .orElseThrow(() -> new ResourceNotFoundException("Sender account not found"));
    ensureAccountOwnership(sender, currentUser);

    // This endpoint records a simulated payment instruction; no balance is debited yet.
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
    ensureAccountOwnership(account, currentUser);

    return account;
  }

  private Account getOwnedAccountForUpdate(Long accountId, User currentUser) {
    Account account = lockAccountOrThrow(accountId);

    ensureAccountOwnership(account, currentUser);

    return account;
  }

  private Account lockAccountOrThrow(Long accountId) {
    try {
      return accountRepository
          .findByIdForUpdate(accountId)
          .orElseThrow(
              () -> new ResourceNotFoundException("Account not found with id: " + accountId));
    } catch (PessimisticLockingFailureException exception) {
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
      long candidate =
          1_000_000_000L + (long) (secureRandom.nextDouble() * 9_000_000_000L);
      accountNumber = String.valueOf(candidate);
    } while (accountRepository.existsByAccountNumber(accountNumber));

    return accountNumber;
  }

  private AccountResponse toAccountResponse(Account account) {
    return new AccountResponse(
        account.getId(),
        account.getAccountNumber(),
        account.getAccountType(),
        account.getBalance(),
        account.getAvailableBalance(),
        account.getHoldAmount(),
        account.getCurrency(),
        account.getStatus());
  }

  private void ensureAccountOwnership(Account account, User user) {
    if (!account.getUser().getId().equals(user.getId())) {
      throw new UnauthorizedAccessException("You do not have access to this account");
    }
  }

  private void ensureSufficientBalance(
      Account account, BigDecimal amount, String operation) {
    if (account.getAvailableBalance().compareTo(amount) < 0) {
      throw new InsufficientBalanceException(
          "Insufficient balance for this " + operation);
    }
  }

  private Transaction saveSuccessfulTransaction(
      TransactionType type, BigDecimal amount, Account sender, Account receiver) {
    Transaction transaction = new Transaction(type, amount, sender, receiver);
    transaction.setStatus(TransactionStatus.SUCCESS);
    return transactionRepository.save(transaction);
  }

  private void ensureActive(Account account) {
    if (account.getStatus() != AccountStatus.ACTIVE) {
      throw new InvalidAccountOperationException("Account is not active");
    }
  }

  private void saveLedger(Transaction transaction, Account account, LedgerEntryType type,
      BigDecimal amount, BigDecimal before, BigDecimal after) {
    if (ledgerEntryRepository != null) {
      ledgerEntryRepository.save(new LedgerEntry(transaction, account, type, amount,
          account.getCurrency(), before, after));
    }
  }
}
