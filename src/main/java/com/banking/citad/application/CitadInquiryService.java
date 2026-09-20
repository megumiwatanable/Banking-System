package com.banking.citad.application;

import com.banking.citad.api.dto.CitadInquiryResponse;
import com.banking.citad.api.dto.CreateCitadInquiryRequest;
import com.banking.citad.domain.CitadInquiry;
import com.banking.citad.domain.CitadStatus;
import com.banking.citad.infrastructure.CitadInquiryRepository;
import com.banking.shared.error.InvalidTransactionException;
import com.banking.shared.error.ResourceAlreadyExistsException;
import com.banking.shared.error.ResourceNotFoundException;
import com.banking.shared.error.UnauthorizedAccessException;
import com.banking.shared.security.SecurityUtils;
import com.banking.transaction.domain.Transaction;
import com.banking.transaction.domain.TransactionType;
import com.banking.transaction.infrastructure.TransactionRepository;
import com.banking.user.domain.User;
import com.banking.user.infrastructure.UserRepository;
import java.security.SecureRandom;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CitadInquiryService {
  private static final String PROCESSING_MODE = "SIMULATED";
  private static final long REFERENCE_RANGE = 9_000_000_000L;
  private static final long REFERENCE_BASE = 1_000_000_000L;

  private final CitadInquiryRepository inquiryRepository;
  private final TransactionRepository transactionRepository;
  private final UserRepository userRepository;
  private final SecureRandom random = new SecureRandom();

  public CitadInquiryService(
      CitadInquiryRepository inquiryRepository,
      TransactionRepository transactionRepository,
      UserRepository userRepository) {
    this.inquiryRepository = inquiryRepository;
    this.transactionRepository = transactionRepository;
    this.userRepository = userRepository;
  }

  public List<CitadInquiryResponse> list() {
    return inquiryRepository
        .findByUserEmailOrderByCreatedAtDesc(SecurityUtils.getCurrentUserEmail())
        .stream()
        .map(this::toResponse)
        .toList();
  }

  public CitadInquiryResponse get(Long id) {
    return toResponse(
        inquiryRepository
            .findByIdAndUserEmail(id, SecurityUtils.getCurrentUserEmail())
            .orElseThrow(() -> new ResourceNotFoundException("CITAD inquiry not found")));
  }

  @Transactional
  public CitadInquiryResponse create(CreateCitadInquiryRequest request) {
    String email = SecurityUtils.getCurrentUserEmail();
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    Transaction t =
        transactionRepository
            .findById(request.transactionId())
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    boolean owned =
        t.getSenderAccount() != null && t.getSenderAccount().getUser().getId().equals(user.getId());
    if (!owned) throw new UnauthorizedAccessException("You cannot dispute this transaction");
    if (t.getTransactionType() != TransactionType.INTERBANK_TRANSFER)
      throw new InvalidTransactionException("CITAD inquiry requires an interbank transaction");
    if (inquiryRepository.existsByTransactionIdAndUserEmail(t.getId(), email))
      throw new ResourceAlreadyExistsException("An inquiry already exists for this transaction");
    CitadInquiry inquiry = new CitadInquiry();
    inquiry.setReferenceNumber(reference());
    inquiry.setUser(user);
    inquiry.setTransaction(t);
    inquiry.setReason(request.reason().trim());
    inquiry.setStatus(CitadStatus.RECEIVED);
    return toResponse(inquiryRepository.save(inquiry));
  }

  private String reference() {
    String value;
    do {
      value = "CITAD" + (REFERENCE_BASE + Math.floorMod(random.nextLong(), REFERENCE_RANGE));
    } while (inquiryRepository.existsByReferenceNumber(value));
    return value;
  }

  private CitadInquiryResponse toResponse(CitadInquiry inquiry) {
    return new CitadInquiryResponse(
        inquiry.getId(),
        inquiry.getReferenceNumber(),
        inquiry.getTransaction().getId(),
        inquiry.getStatus(),
        inquiry.getReason(),
        inquiry.getBankResponse(),
        inquiry.getCreatedAt(),
        inquiry.getUpdatedAt(),
        PROCESSING_MODE);
  }
}
