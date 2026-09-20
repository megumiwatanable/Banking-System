package com.banking.citad.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.banking.account.domain.*;
import com.banking.citad.api.dto.CreateCitadInquiryRequest;
import com.banking.citad.domain.*;
import com.banking.citad.infrastructure.CitadInquiryRepository;
import com.banking.shared.error.InvalidTransactionException;
import com.banking.shared.security.SecurityUtils;
import com.banking.transaction.domain.*;
import com.banking.transaction.infrastructure.TransactionRepository;
import com.banking.user.domain.User;
import com.banking.user.infrastructure.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CitadInquiryServiceTest {
  @Mock CitadInquiryRepository inquiries;
  @Mock TransactionRepository transactions;
  @Mock UserRepository users;
  MockedStatic<SecurityUtils> security;
  CitadInquiryService service;
  User user;
  Account account;

  @BeforeEach
  void setUp() {
    service = new CitadInquiryService(inquiries, transactions, users);
    user = new User();
    user.setId(1L);
    user.setEmail("a@b.com");
    account = new Account("1234567890", AccountType.CURRENT, user);
    account.setId(2L);
    security = mockStatic(SecurityUtils.class);
    security.when(SecurityUtils::getCurrentUserEmail).thenReturn(user.getEmail());
    when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
  }

  @AfterEach
  void close() {
    security.close();
  }

  @Test
  void createsInquiryForOwnedInterbankTransaction() {
    Transaction tx =
        new Transaction(TransactionType.INTERBANK_TRANSFER, BigDecimal.TEN, account, null);
    tx.setId(8L);
    when(transactions.findById(8L)).thenReturn(Optional.of(tx));
    when(inquiries.save(any()))
        .thenAnswer(
            i -> {
              CitadInquiry q = i.getArgument(0);
              q.setId(9L);
              return q;
            });
    var result = service.create(new CreateCitadInquiryRequest(8L, "Wrong recipient"));
    assertEquals(CitadStatus.RECEIVED, result.status());
    assertEquals("SIMULATED", result.processingMode());
  }

  @Test
  void rejectsNonInterbankTransaction() {
    Transaction tx = new Transaction(TransactionType.TRANSFER, BigDecimal.TEN, account, null);
    tx.setId(8L);
    when(transactions.findById(8L)).thenReturn(Optional.of(tx));
    assertThrows(
        InvalidTransactionException.class,
        () -> service.create(new CreateCitadInquiryRequest(8L, "Wrong recipient")));
  }
}
