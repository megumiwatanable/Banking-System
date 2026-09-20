package com.banking.transfer.application;

import com.banking.account.domain.Account;
import com.banking.account.infrastructure.AccountRepository;
import com.banking.ledger.domain.LedgerEntry;
import com.banking.ledger.domain.LedgerEntryType;
import com.banking.ledger.infrastructure.LedgerEntryRepository;
import com.banking.shared.error.InsufficientBalanceException;
import com.banking.shared.error.InvalidTransactionException;
import com.banking.shared.error.ResourceNotFoundException;
import com.banking.shared.error.UnauthorizedAccessException;
import com.banking.shared.security.SecurityUtils;
import com.banking.transaction.domain.Transaction;
import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.domain.TransactionType;
import com.banking.transaction.infrastructure.TransactionRepository;
import com.banking.transfer.api.dto.InternalTransferRequest;
import com.banking.transfer.api.dto.TransferResult;
import com.banking.transfer.domain.IdempotencyRecord;
import com.banking.transfer.infrastructure.IdempotencyRecordRepository;
import com.banking.user.domain.User;
import com.banking.user.domain.UserStatus;
import com.banking.user.infrastructure.UserRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates atomic balance, transaction, ledger, and idempotency updates. */
@Service
public class InternalTransferService {
  private final AccountRepository accountRepository;
  private final TransactionRepository transactionRepository;
  private final LedgerEntryRepository ledgerRepository;
  private final UserRepository userRepository;
  private final IdempotencyRecordRepository idempotencyRepository;
  private final TransferRequestHasher requestHasher;
  private final TransferPolicy transferPolicy;

  public InternalTransferService(
      AccountRepository accountRepository,
      TransactionRepository transactionRepository,
      LedgerEntryRepository ledgerRepository,
      UserRepository userRepository,
      IdempotencyRecordRepository idempotencyRepository,
      TransferRequestHasher requestHasher,
      TransferPolicy transferPolicy) {
    this.accountRepository = accountRepository;
    this.transactionRepository = transactionRepository;
    this.ledgerRepository = ledgerRepository;
    this.userRepository = userRepository;
    this.idempotencyRepository = idempotencyRepository;
    this.requestHasher = requestHasher;
    this.transferPolicy = transferPolicy;
  }

  @Transactional
  public TransferResult transfer(String idempotencyKey, InternalTransferRequest request) {
    validateRequestIdentity(idempotencyKey, request);
    User customer = getCurrentCustomer();
    String requestHash = requestHasher.hash(request);

    TransferResult previousResult =
        findPreviousResult(customer, idempotencyKey, requestHash);
    if (previousResult != null) {
      return previousResult;
    }

    Account sourceReference = findAccount(request.fromAccount(), "Source account not found");
    verifySourceOwnership(sourceReference, customer);
    Account destinationReference =
        findAccount(request.toAccount(), "Destination account not found");
    LockedAccounts locked = lockInDeterministicOrder(sourceReference, destinationReference);

    transferPolicy.validate(locked.source(), locked.destination(), request, customer);
    IdempotencyRecord marker =
        createProcessingMarker(customer, idempotencyKey, requestHash);
    Transaction transaction = moveMoney(locked.source(), locked.destination(), request);

    marker.setTransaction(transaction);
    marker.setStatus("SUCCESS");
    idempotencyRepository.save(marker);
    return toResult(transaction);
  }

  @Transactional
  public TransferResult reverse(Long transactionId) {
    User customer = getCurrentCustomer();
    Transaction original = findReversibleTransaction(transactionId, customer);
    LockedAccounts locked =
        lockInDeterministicOrder(original.getSenderAccount(), original.getReceiverAccount());
    Account originalSource = locked.accountById(original.getSenderAccount().getId());
    Account originalDestination = locked.accountById(original.getReceiverAccount().getId());

    if (originalDestination.getAvailableBalance().compareTo(original.getAmount()) < 0) {
      throw new InsufficientBalanceException("Recipient balance is insufficient for reversal");
    }
    return toResult(reverseMoney(original, originalSource, originalDestination));
  }

  private Transaction moveMoney(
      Account source, Account destination, InternalTransferRequest request) {
    BigDecimal fee = transferPolicy.fee();
    BigDecimal sourceBefore = source.getBalance();
    BigDecimal destinationBefore = destination.getBalance();
    source.setBalance(sourceBefore.subtract(request.amount().add(fee)));
    destination.setBalance(destinationBefore.add(request.amount()));

    Transaction transaction =
        new Transaction(TransactionType.INTERNAL_TRANSFER, request.amount(), source, destination);
    transaction.setStatus(TransactionStatus.SUCCESS);
    transaction.setCurrency(request.currency());
    transaction.setFee(fee);
    transaction.setDescription(request.description());
    transaction = transactionRepository.save(transaction);

    accountRepository.save(source);
    accountRepository.save(destination);
    writeTransferLedger(
        transaction, source, destination, request.amount(), fee, sourceBefore, destinationBefore);
    return transaction;
  }

  private Transaction reverseMoney(
      Transaction original,
      Account originalSource,
      Account originalDestination
  ) {
    BigDecimal sourceBefore = originalSource.getBalance();
    BigDecimal destinationBefore = originalDestination.getBalance();
    BigDecimal refund = original.getAmount().add(original.getFee());
    originalDestination.setBalance(destinationBefore.subtract(original.getAmount()));
    originalSource.setBalance(sourceBefore.add(refund));

    Transaction reversal =
        new Transaction(
            TransactionType.REVERSAL,
            original.getAmount(),
            originalDestination,
            originalSource);
    reversal.setCurrency(original.getCurrency());
    reversal.setStatus(TransactionStatus.SUCCESS);
    reversal.setOriginalTransaction(original);
    reversal = transactionRepository.save(reversal);

    // Keep the original movement; correction is represented by compensating entries.
    original.setStatus(TransactionStatus.REVERSED);
    accountRepository.save(originalSource);
    accountRepository.save(originalDestination);
    writeReversalLedger(
        reversal,
        original,
        originalSource,
        originalDestination,
        sourceBefore,
        destinationBefore,
        refund);
    return reversal;
  }

  private void writeTransferLedger(
      Transaction transaction,
      Account source,
      Account destination,
      BigDecimal amount,
      BigDecimal fee,
      BigDecimal sourceBefore,
      BigDecimal destinationBefore) {
    ledgerRepository.save(
        new LedgerEntry(
            transaction,
            source,
            LedgerEntryType.DEBIT,
            amount,
            source.getCurrency(),
            sourceBefore,
            sourceBefore.subtract(amount)));
    if (fee.signum() > 0) {
      ledgerRepository.save(
          new LedgerEntry(
              transaction,
              source,
              LedgerEntryType.FEE,
              fee,
              source.getCurrency(),
              sourceBefore.subtract(amount),
              source.getBalance()));
    }
    ledgerRepository.save(
        new LedgerEntry(
            transaction,
            destination,
            LedgerEntryType.CREDIT,
            amount,
            destination.getCurrency(),
            destinationBefore,
            destination.getBalance()));
  }

  private void writeReversalLedger(
      Transaction reversal,
      Transaction original,
      Account source,
      Account destination,
      BigDecimal sourceBefore,
      BigDecimal destinationBefore,
      BigDecimal refund) {
    ledgerRepository.save(
        new LedgerEntry(
            reversal,
            destination,
            LedgerEntryType.REVERSAL,
            original.getAmount(),
            original.getCurrency(),
            destinationBefore,
            destination.getBalance()));
    ledgerRepository.save(
        new LedgerEntry(
            reversal,
            source,
            LedgerEntryType.REVERSAL,
            refund,
            original.getCurrency(),
            sourceBefore,
            source.getBalance()));
  }

  private LockedAccounts lockInDeterministicOrder(Account source, Account destination) {
    long firstId = Math.min(source.getId(), destination.getId());
    long secondId = Math.max(source.getId(), destination.getId());

    // A global lock order prevents deadlocks between opposite-direction transfers.
    Account first = lockAccount(firstId);
    Account second = lockAccount(secondId);
    return source.getId().equals(firstId)
        ? new LockedAccounts(first, second)
        : new LockedAccounts(second, first);
  }

  private TransferResult findPreviousResult(User customer, String key, String requestHash) {
    var existing = idempotencyRepository.findByUserIdAndKey(customer.getId(), key);
    if (existing.isEmpty()) {
      return null;
    }
    IdempotencyRecord record = existing.get();
    if (!record.getRequestHash().equals(requestHash)) {
      throw new InvalidTransactionException(
          "Idempotency-Key was already used with a different request");
    }
    if (record.getTransaction() == null) {
      throw new InvalidTransactionException("Transaction is still processing");
    }
    return toResult(record.getTransaction());
  }

  private IdempotencyRecord createProcessingMarker(User customer, String key, String hash) {
    IdempotencyRecord record = new IdempotencyRecord();
    record.setKey(key);
    record.setUser(customer);
    record.setRequestHash(hash);
    record.setStatus("PROCESSING");
    return idempotencyRepository.save(record);
  }

  private Transaction findReversibleTransaction(Long transactionId, User customer) {
    Transaction transaction =
        transactionRepository
            .findById(transactionId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    if (transaction.getTransactionType() != TransactionType.INTERNAL_TRANSFER
        || transaction.getStatus() != TransactionStatus.SUCCESS) {
      throw new InvalidTransactionException(
          "Only a successful internal transfer can be reversed");
    }
    if (transaction.getSenderAccount() == null
        || !transaction.getSenderAccount().getUser().getId().equals(customer.getId())) {
      throw new UnauthorizedAccessException("Only the transfer owner can request reversal");
    }
    return transaction;
  }

  private User getCurrentCustomer() {
    User customer =
        userRepository
            .findByEmail(SecurityUtils.getCurrentUserEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    if (customer.getStatus() != UserStatus.ACTIVE) {
      throw new UnauthorizedAccessException("Customer is not active");
    }
    return customer;
  }

  private Account findAccount(String accountNumber, String errorMessage) {
    return accountRepository
        .findByAccountNumber(accountNumber)
        .orElseThrow(() -> new ResourceNotFoundException(errorMessage));
  }

  private Account lockAccount(Long accountId) {
    return accountRepository
        .findByIdForUpdate(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
  }

  private void verifySourceOwnership(Account source, User customer) {
    if (!source.getUser().getId().equals(customer.getId())) {
      throw new UnauthorizedAccessException("Source account does not belong to you");
    }
  }

  private void validateRequestIdentity(String key, InternalTransferRequest request) {
    if (key == null || key.isBlank() || key.length() > 100) {
      throw new InvalidTransactionException("A valid Idempotency-Key is required");
    }
    if (request.fromAccount().equals(request.toAccount())) {
      throw new InvalidTransactionException("Source and destination accounts must differ");
    }
  }

  private TransferResult toResult(Transaction transaction) {
    return new TransferResult(
        transaction.getId(),
        transaction.getStatus(),
        transaction.getAmount(),
        transaction.getFee(),
        transaction.getCurrency(),
        transaction.getTransactionTime());
  }

  private record LockedAccounts(Account source, Account destination) {
    private Account accountById(Long accountId) {
      return source.getId().equals(accountId) ? source : destination;
    }
  }
}
