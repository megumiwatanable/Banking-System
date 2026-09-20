package com.banking.account.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.account.api.dto.CreateAccountRequest;
import com.banking.account.api.dto.DepositRequest;
import com.banking.account.api.dto.InterbankTransferRequest;
import com.banking.account.api.dto.TransferRequest;
import com.banking.account.api.dto.WithdrawRequest;
import com.banking.account.domain.Account;
import com.banking.account.domain.AccountType;
import com.banking.account.infrastructure.AccountRepository;
import com.banking.shared.error.AccountLockTimeoutException;
import com.banking.shared.error.InsufficientBalanceException;
import com.banking.shared.error.InvalidAccountOperationException;
import com.banking.shared.error.ResourceNotFoundException;
import com.banking.shared.error.UnauthorizedAccessException;
import com.banking.shared.security.SecurityUtils;
import com.banking.transaction.domain.Transaction;
import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.infrastructure.TransactionRepository;
import com.banking.user.domain.User;
import com.banking.user.infrastructure.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.PessimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

  @Mock private AccountRepository accountRepository;

  @Mock private TransactionRepository transactionRepository;

  @Mock private UserRepository userRepository;

  private AccountServiceImpl accountService;

  private MockedStatic<SecurityUtils> securityUtils;

  private User owner;
  private User otherUser;

  @BeforeEach
  void setUp() {
    accountService =
        new AccountServiceImpl(accountRepository, transactionRepository, userRepository);

    owner = new User();
    owner.setId(1L);
    owner.setEmail("owner@bank.com");

    otherUser = new User();
    otherUser.setId(2L);
    otherUser.setEmail("other@bank.com");

    securityUtils = mockStatic(SecurityUtils.class);
  }

  @AfterEach
  void tearDown() {
    securityUtils.close();
  }

  private void loginAs(User user) {
    securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(user.getEmail());
    lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
  }

  private Account accountFor(Long id, User user, BigDecimal balance) {
    Account account = new Account("100000000" + id, AccountType.SAVINGS, user);
    account.setId(id);
    account.setBalance(balance);

    account.setVersion(0L);

    return account;
  }

  @Test
  void createGeneratesAccountWithZeroBalance() {
    loginAs(owner);
    when(accountRepository.existsByAccountNumber(any())).thenReturn(false);
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(
            inv -> {
              Account a = inv.getArgument(0);
              a.setId(10L);
              return a;
            });

    var response = accountService.create(new CreateAccountRequest(AccountType.SAVINGS));

    assertEquals(BigDecimal.ZERO, response.balance());
    verify(accountRepository).save(any(Account.class));
  }

  @Test
  void getAccountByIdThrowsWhenNotOwner() {
    loginAs(owner);
    Account account = accountFor(5L, otherUser, BigDecimal.TEN);
    when(accountRepository.findById(5L)).thenReturn(Optional.of(account));

    assertThrows(UnauthorizedAccessException.class, () -> accountService.getAccountById(5L));
  }

  @Test
  void getAccountByIdThrowsWhenMissing() {
    loginAs(owner);
    when(accountRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> accountService.getAccountById(99L));
  }

  @Test
  void depositIncreasesBalanceUsingLockedRead() {
    loginAs(owner);
    Account account = accountFor(5L, owner, new BigDecimal("100.00"));
    when(accountRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(account));
    when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

    var response = accountService.deposit(5L, new DepositRequest(new BigDecimal("50.00")));

    assertEquals(new BigDecimal("150.00"), response.balance());
    verify(accountRepository).findByIdForUpdate(5L);
    verify(accountRepository, never()).findById(5L);
    verify(transactionRepository).save(any());
  }

  @Test
  void withdrawThrowsWhenBalanceTooLow() {
    loginAs(owner);
    Account account = accountFor(5L, owner, new BigDecimal("30.00"));
    when(accountRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(account));

    assertThrows(
        InsufficientBalanceException.class,
        () -> accountService.withdraw(5L, new WithdrawRequest(new BigDecimal("50.00"))));

    verify(accountRepository, never()).save(any());
    verify(transactionRepository, never()).save(any());
  }

  @Test
  void withdrawSucceedsWhenFundsAreSufficient() {
    loginAs(owner);
    Account account = accountFor(5L, owner, new BigDecimal("100.00"));
    when(accountRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(account));
    when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

    var response = accountService.withdraw(5L, new WithdrawRequest(new BigDecimal("40.00")));

    assertEquals(new BigDecimal("60.00"), response.balance());
  }

  @Test
  void depositWrapsPessimisticLockFailure() {
    loginAs(owner);
    when(accountRepository.findByIdForUpdate(5L))
        .thenThrow(new PessimisticLockingFailureException("row locked"));

    assertThrows(
        AccountLockTimeoutException.class,
        () -> accountService.deposit(5L, new DepositRequest(new BigDecimal("10.00"))));
  }

  @Test
  void transferRejectsSameAccount() {
    loginAs(owner);

    assertThrows(
        InvalidAccountOperationException.class,
        () ->
            accountService.transfer(
                new TransferRequest("1000000005", "1000000005", new BigDecimal("10.00"))));

    verify(accountRepository, never()).findByIdForUpdate(anyLong());
  }

  @Test
  void transferLocksAccountsInAscendingIdOrderRegardlessOfSenderReceiver() {
    loginAs(owner);
    Account sender = accountFor(9L, owner, new BigDecimal("200.00"));
    Account receiver = accountFor(3L, otherUser, new BigDecimal("50.00"));
    when(accountRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(receiver));
    when(accountRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(sender));
    when(accountRepository.findByAccountNumber(sender.getAccountNumber()))
        .thenReturn(Optional.of(sender));
    when(accountRepository.findByAccountNumber(receiver.getAccountNumber()))
        .thenReturn(Optional.of(receiver));
    when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

    accountService.transfer(
        new TransferRequest(
            sender.getAccountNumber(), receiver.getAccountNumber(), new BigDecimal("25.00")));

    var inOrder = Mockito.inOrder(accountRepository);
    inOrder.verify(accountRepository).findByIdForUpdate(3L);
    inOrder.verify(accountRepository).findByIdForUpdate(9L);
  }

  @Test
  void transferThrowsWhenSenderNotOwnedByCaller() {
    loginAs(owner);
    Account sender = accountFor(3L, otherUser, new BigDecimal("200.00"));
    Account receiver = accountFor(9L, owner, new BigDecimal("50.00"));
    when(accountRepository.findByAccountNumber(sender.getAccountNumber()))
        .thenReturn(Optional.of(sender));

    assertThrows(
        UnauthorizedAccessException.class,
        () ->
            accountService.transfer(
                new TransferRequest(
                    sender.getAccountNumber(),
                    receiver.getAccountNumber(),
                    new BigDecimal("10.00"))));

    verify(accountRepository, never()).save(any());
  }

  @Test
  void transferThrowsWhenSenderBalanceTooLow() {
    loginAs(owner);
    Account sender = accountFor(3L, owner, new BigDecimal("10.00"));
    Account receiver = accountFor(9L, otherUser, new BigDecimal("50.00"));
    when(accountRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(sender));
    when(accountRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(receiver));
    when(accountRepository.findByAccountNumber(sender.getAccountNumber()))
        .thenReturn(Optional.of(sender));
    when(accountRepository.findByAccountNumber(receiver.getAccountNumber()))
        .thenReturn(Optional.of(receiver));

    assertThrows(
        InsufficientBalanceException.class,
        () ->
            accountService.transfer(
                new TransferRequest(
                    sender.getAccountNumber(),
                    receiver.getAccountNumber(),
                    new BigDecimal("40.00"))));
  }

  @Test
  void transferMovesFundsBetweenBothAccounts() {
    loginAs(owner);
    Account sender = accountFor(3L, owner, new BigDecimal("100.00"));
    Account receiver = accountFor(9L, otherUser, new BigDecimal("50.00"));
    when(accountRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(sender));
    when(accountRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(receiver));
    when(accountRepository.findByAccountNumber(sender.getAccountNumber()))
        .thenReturn(Optional.of(sender));
    when(accountRepository.findByAccountNumber(receiver.getAccountNumber()))
        .thenReturn(Optional.of(receiver));
    when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

    var response =
        accountService.transfer(
            new TransferRequest(
                sender.getAccountNumber(), receiver.getAccountNumber(), new BigDecimal("30.00")));

    assertEquals(new BigDecimal("70.00"), response.balance());
    assertEquals(new BigDecimal("80.00"), receiver.getBalance());
    verify(transactionRepository).save(any());
  }

  @Test
  void interbankRequestIsPendingAndDoesNotDebitSender() {
    loginAs(owner);
    Account sender = accountFor(3L, owner, new BigDecimal("100.00"));
    when(accountRepository.findByAccountNumber(sender.getAccountNumber()))
        .thenReturn(Optional.of(sender));
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(
            inv -> {
              Transaction transaction = inv.getArgument(0);
              transaction.setId(42L);
              return transaction;
            });

    var response =
        accountService.requestInterbankTransfer(
            new InterbankTransferRequest(
                sender.getAccountNumber(),
                "VCB",
                "0123456789",
                "Nguyen Van B",
                new BigDecimal("30.00")));

    assertEquals(TransactionStatus.PENDING, response.status());
    assertEquals(42L, response.transactionId());
    assertEquals(new BigDecimal("100.00"), sender.getBalance());
    verify(accountRepository, never()).save(any(Account.class));
  }
}
