package com.banking.hold.application;

import com.banking.account.domain.Account;
import com.banking.account.domain.AccountStatus;
import com.banking.account.infrastructure.AccountRepository;
import com.banking.hold.api.dto.HoldRequest;
import com.banking.hold.api.dto.HoldResponse;
import com.banking.hold.domain.BalanceHold;
import com.banking.hold.domain.HoldStatus;
import com.banking.hold.infrastructure.BalanceHoldRepository;
import com.banking.ledger.domain.LedgerEntry;
import com.banking.ledger.domain.LedgerEntryType;
import com.banking.ledger.infrastructure.LedgerEntryRepository;
import com.banking.shared.error.InsufficientBalanceException;
import com.banking.shared.error.InvalidAccountOperationException;
import com.banking.shared.error.InvalidTransactionException;
import com.banking.shared.error.ResourceAlreadyExistsException;
import com.banking.shared.error.ResourceNotFoundException;
import com.banking.shared.error.UnauthorizedAccessException;
import com.banking.shared.security.SecurityUtils;
import com.banking.transaction.domain.Transaction;
import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.domain.TransactionType;
import com.banking.transaction.infrastructure.TransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BalanceHoldService {
  private final BalanceHoldRepository holdRepository;
  private final AccountRepository accountRepository;
  private final TransactionRepository transactionRepository;
  private final LedgerEntryRepository ledgerRepository;

  public BalanceHoldService(
      BalanceHoldRepository holdRepository,
      AccountRepository accountRepository,
      TransactionRepository transactionRepository,
      LedgerEntryRepository ledgerRepository) {
    this.holdRepository = holdRepository;
    this.accountRepository = accountRepository;
    this.transactionRepository = transactionRepository;
    this.ledgerRepository = ledgerRepository;
  }

  @Transactional(readOnly = true)
  public List<HoldResponse> list() {
    return holdRepository
        .findByAccountUserEmailOrderByCreatedAtDesc(SecurityUtils.getCurrentUserEmail())
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public HoldResponse hold(HoldRequest request) {
    if (holdRepository.existsByReference(request.reference())) {
      throw new ResourceAlreadyExistsException("Hold reference already exists");
    }

    Account account = getOwnedAccountForUpdate(request.accountId());
    validateAccount(account, request.currency());
    if (account.getAvailableBalance().compareTo(request.amount()) < 0) {
      throw new InsufficientBalanceException("Available balance is insufficient");
    }

    Transaction transaction = createPendingTransaction(account, request);
    // A hold reduces only available balance; booked balance changes on capture.
    account.setHoldAmount(account.getHoldAmount().add(request.amount()));
    accountRepository.save(account);

    BalanceHold hold = new BalanceHold();
    hold.setAccount(account);
    hold.setTransaction(transaction);
    hold.setAmount(request.amount());
    hold.setCurrency(request.currency());
    hold.setReference(request.reference());
    hold.setStatus(HoldStatus.HELD);
    hold = holdRepository.save(hold);

    writeLedger(hold, LedgerEntryType.HOLD, account.getBalance(), account.getBalance());
    return toResponse(hold);
  }

  @Transactional
  public HoldResponse release(Long holdId) {
    BalanceHold hold = getOwnedHold(holdId);
    ensureHeld(hold);
    Account account = lockAccount(hold.getAccount().getId());

    account.setHoldAmount(account.getHoldAmount().subtract(hold.getAmount()));
    hold.setStatus(HoldStatus.RELEASED);
    hold.getTransaction().setStatus(TransactionStatus.FAILED);
    writeLedger(hold, LedgerEntryType.RELEASE, account.getBalance(), account.getBalance());
    return toResponse(holdRepository.save(hold));
  }

  @Transactional
  public HoldResponse capture(Long holdId) {
    BalanceHold hold = getOwnedHold(holdId);
    ensureHeld(hold);
    Account account = lockAccount(hold.getAccount().getId());
    BigDecimal balanceBefore = account.getBalance();

    // Capture consumes both the reservation and the corresponding booked funds atomically.
    account.setHoldAmount(account.getHoldAmount().subtract(hold.getAmount()));
    account.setBalance(balanceBefore.subtract(hold.getAmount()));
    hold.setStatus(HoldStatus.CAPTURED);
    hold.getTransaction().setStatus(TransactionStatus.SUCCESS);
    writeLedger(hold, LedgerEntryType.DEBIT, balanceBefore, account.getBalance());
    return toResponse(holdRepository.save(hold));
  }

  private Transaction createPendingTransaction(Account account, HoldRequest request) {
    Transaction transaction =
        new Transaction(TransactionType.INTERBANK_TRANSFER, request.amount(), account, null);
    transaction.setCurrency(request.currency());
    transaction.setStatus(TransactionStatus.PENDING);
    return transactionRepository.save(transaction);
  }

  private void writeLedger(
      BalanceHold hold,
      LedgerEntryType entryType,
      BigDecimal balanceBefore,
      BigDecimal balanceAfter) {
    ledgerRepository.save(
        new LedgerEntry(
            hold.getTransaction(),
            hold.getAccount(),
            entryType,
            hold.getAmount(),
            hold.getCurrency(),
            balanceBefore,
            balanceAfter));
  }

  private BalanceHold getOwnedHold(Long holdId) {
    return holdRepository
        .findByIdAndAccountUserEmail(holdId, SecurityUtils.getCurrentUserEmail())
        .orElseThrow(() -> new ResourceNotFoundException("Balance hold not found"));
  }

  private Account getOwnedAccountForUpdate(Long accountId) {
    Account account = lockAccount(accountId);
    if (!account.getUser().getEmail().equals(SecurityUtils.getCurrentUserEmail())) {
      throw new UnauthorizedAccessException("You do not have access to this account");
    }
    return account;
  }

  private Account lockAccount(Long accountId) {
    return accountRepository
        .findByIdForUpdate(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
  }

  private void validateAccount(Account account, String currency) {
    if (account.getStatus() != AccountStatus.ACTIVE) {
      throw new InvalidAccountOperationException("Account is not active");
    }
    if (!account.getCurrency().equals(currency)) {
      throw new InvalidTransactionException("Currency mismatch");
    }
  }

  private void ensureHeld(BalanceHold hold) {
    if (hold.getStatus() != HoldStatus.HELD) {
      throw new InvalidTransactionException("Only a held balance can be changed");
    }
  }

  private HoldResponse toResponse(BalanceHold hold) {
    return new HoldResponse(
        hold.getId(),
        hold.getAccount().getId(),
        hold.getAmount(),
        hold.getCurrency(),
        hold.getStatus(),
        hold.getReference());
  }
}
