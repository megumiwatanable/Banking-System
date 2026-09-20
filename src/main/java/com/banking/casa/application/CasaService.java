package com.banking.casa.application;

import com.banking.account.domain.Account;
import com.banking.account.domain.AccountType;
import com.banking.account.infrastructure.AccountRepository;
import com.banking.casa.api.dto.CasaEnrollmentRequest;
import com.banking.casa.api.dto.CasaEnrollmentResponse;
import com.banking.casa.domain.CasaEnrollment;
import com.banking.casa.domain.CasaStatus;
import com.banking.casa.infrastructure.CasaEnrollmentRepository;
import com.banking.shared.error.InvalidAccountOperationException;
import com.banking.shared.error.ResourceAlreadyExistsException;
import com.banking.shared.error.ResourceNotFoundException;
import com.banking.shared.error.UnauthorizedAccessException;
import com.banking.shared.security.SecurityUtils;
import com.banking.user.domain.User;
import com.banking.user.infrastructure.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CasaService {
  private final CasaEnrollmentRepository enrollmentRepository;
  private final AccountRepository accountRepository;
  private final UserRepository userRepository;

  public CasaService(
      CasaEnrollmentRepository enrollmentRepository,
      AccountRepository accountRepository,
      UserRepository userRepository) {
    this.enrollmentRepository = enrollmentRepository;
    this.accountRepository = accountRepository;
    this.userRepository = userRepository;
  }

  public CasaEnrollmentResponse current() {
    return enrollmentRepository
        .findByUserEmail(SecurityUtils.getCurrentUserEmail())
        .map(this::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("CASA enrollment not found"));
  }

  @Transactional
  public CasaEnrollmentResponse enroll(CasaEnrollmentRequest request) {
    String email = SecurityUtils.getCurrentUserEmail();
    if (enrollmentRepository.findByUserEmail(email).isPresent())
      throw new ResourceAlreadyExistsException("Customer already joined CASA");
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    Account account =
        accountRepository
            .findByAccountNumber(request.settlementAccountNumber())
            .orElseThrow(() -> new ResourceNotFoundException("Settlement account not found"));
    if (!account.getUser().getId().equals(user.getId()))
      throw new UnauthorizedAccessException("You do not own this account");
    if (account.getAccountType() != AccountType.CURRENT)
      throw new InvalidAccountOperationException("CASA requires a CURRENT settlement account");
    CasaEnrollment enrollment = new CasaEnrollment();
    enrollment.setUser(user);
    enrollment.setSettlementAccount(account);
    enrollment.setPackageType(request.packageType());
    enrollment.setStatus(CasaStatus.ACTIVE);
    return toResponse(enrollmentRepository.save(enrollment));
  }

  private CasaEnrollmentResponse toResponse(CasaEnrollment enrollment) {
    return new CasaEnrollmentResponse(
        enrollment.getId(),
        enrollment.getSettlementAccount().getAccountNumber(),
        enrollment.getPackageType(),
        enrollment.getStatus(),
        enrollment.getEnrolledAt());
  }
}
