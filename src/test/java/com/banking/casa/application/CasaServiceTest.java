package com.banking.casa.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.banking.account.domain.*;
import com.banking.account.infrastructure.AccountRepository;
import com.banking.casa.api.dto.CasaEnrollmentRequest;
import com.banking.casa.domain.*;
import com.banking.casa.infrastructure.CasaEnrollmentRepository;
import com.banking.shared.error.InvalidAccountOperationException;
import com.banking.shared.security.SecurityUtils;
import com.banking.user.domain.User;
import com.banking.user.infrastructure.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CasaServiceTest {
  @Mock CasaEnrollmentRepository enrollments;
  @Mock AccountRepository accounts;
  @Mock UserRepository users;
  MockedStatic<SecurityUtils> security;
  CasaService service;
  User user;

  @BeforeEach
  void setUp() {
    service = new CasaService(enrollments, accounts, users);
    user = new User();
    user.setId(1L);
    user.setEmail("a@b.com");
    security = mockStatic(SecurityUtils.class);
    security.when(SecurityUtils::getCurrentUserEmail).thenReturn(user.getEmail());
    when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
  }

  @AfterEach
  void close() {
    security.close();
  }

  @Test
  void enrollsOwnedCurrentAccount() {
    Account account = new Account("1234567890", AccountType.CURRENT, user);
    account.setId(2L);
    when(accounts.findByAccountNumber("1234567890")).thenReturn(Optional.of(account));
    when(enrollments.save(any()))
        .thenAnswer(
            i -> {
              CasaEnrollment e = i.getArgument(0);
              e.setId(3L);
              return e;
            });
    var result = service.enroll(new CasaEnrollmentRequest("1234567890", CasaPackage.BASIC));
    assertEquals(CasaStatus.ACTIVE, result.status());
    assertEquals("1234567890", result.settlementAccountNumber());
  }

  @Test
  void rejectsSavingsAccount() {
    Account account = new Account("1234567890", AccountType.SAVINGS, user);
    when(accounts.findByAccountNumber("1234567890")).thenReturn(Optional.of(account));
    assertThrows(
        InvalidAccountOperationException.class,
        () -> service.enroll(new CasaEnrollmentRequest("1234567890", CasaPackage.BASIC)));
  }
}
