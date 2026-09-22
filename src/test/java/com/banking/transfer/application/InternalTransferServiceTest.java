package com.banking.transfer.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.banking.account.infrastructure.AccountRepository;
import com.banking.ledger.infrastructure.LedgerEntryRepository;
import com.banking.shared.error.InvalidTransactionException;
import com.banking.shared.error.UnauthorizedAccessException;
import com.banking.transaction.domain.*;
import com.banking.transaction.infrastructure.TransactionRepository;
import com.banking.transfer.api.dto.InternalTransferRequest;
import com.banking.transfer.domain.IdempotencyRecord;
import com.banking.transfer.event.TransferEventPublisher;
import com.banking.transfer.infrastructure.*;
import com.banking.user.domain.*;
import com.banking.user.infrastructure.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import com.banking.shared.security.SecurityUtils;

class InternalTransferServiceTest {
  private AccountRepository accounts; private TransactionRepository transactions;
  private LedgerEntryRepository ledger; private UserRepository users;
  private IdempotencyRecordRepository idempotency;
  private TransferRequestHasher requestHasher;
  private TransferPolicy transferPolicy;
  private TransferEventPublisher transferEventPublisher;
  private InternalTransferService service;

  @BeforeEach void setUp() {
    accounts=mock(AccountRepository.class); transactions=mock(TransactionRepository.class);
    ledger=mock(LedgerEntryRepository.class); users=mock(UserRepository.class);
    idempotency = mock(IdempotencyRecordRepository.class);
    requestHasher = mock(TransferRequestHasher.class);
    transferPolicy = mock(TransferPolicy.class);
    transferEventPublisher = mock(TransferEventPublisher.class);
    service =
        new InternalTransferService(
            accounts,
            transactions,
            ledger,
            users,
            idempotency,
            requestHasher,
            transferPolicy,
            transferEventPublisher);
  }

  @Test void rejectsSameIdempotencyKeyWithDifferentPayload() {
    User user=user(UserStatus.ACTIVE); IdempotencyRecord record=new IdempotencyRecord();
    record.setRequestHash("different");
    when(users.findByEmail("owner@bank.test")).thenReturn(Optional.of(user));
    when(requestHasher.hash(any())).thenReturn("actual");
    when(idempotency.findByUserIdAndKey(7L,"key-1")).thenReturn(Optional.of(record));
    try (MockedStatic<SecurityUtils> security=mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getCurrentUserEmail).thenReturn("owner@bank.test");
      assertThatThrownBy(() -> service.transfer("key-1",request("1000000001","1000000002")))
          .isInstanceOf(InvalidTransactionException.class).hasMessageContaining("different request");
    }
    verifyNoInteractions(
        accounts, transactions, ledger, transferPolicy, transferEventPublisher);
  }

  @Test void rejectsInactiveCustomerBeforeTouchingAccounts() {
    User user=user(UserStatus.SUSPENDED);
    when(users.findByEmail("owner@bank.test")).thenReturn(Optional.of(user));
    try (MockedStatic<SecurityUtils> security=mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getCurrentUserEmail).thenReturn("owner@bank.test");
      assertThatThrownBy(() -> service.transfer("key-1",request("1000000001","1000000002")))
          .isInstanceOf(UnauthorizedAccessException.class).hasMessageContaining("not active");
    }
    verifyNoInteractions(
        accounts,
        transactions,
        ledger,
        idempotency,
        transferPolicy,
        transferEventPublisher);
  }

  private User user(UserStatus status) {
    User user = new User();
    user.setId(7L);
    user.setEmail("owner@bank.test");
    user.setStatus(status);
    return user;
  }

  private InternalTransferRequest request(String fromAccount, String toAccount) {
    return new InternalTransferRequest(
        fromAccount, toAccount, new BigDecimal("1000.00"), "VND", "test");
  }
}
