package com.banking.beneficiary.application;

import com.banking.beneficiary.api.dto.BeneficiaryRequest;
import com.banking.beneficiary.api.dto.BeneficiaryResponse;
import com.banking.beneficiary.domain.Beneficiary;
import com.banking.beneficiary.infrastructure.BeneficiaryRepository;
import com.banking.shared.error.ResourceAlreadyExistsException;
import com.banking.shared.error.ResourceNotFoundException;
import com.banking.shared.security.SecurityUtils;
import com.banking.user.domain.User;
import com.banking.user.infrastructure.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BeneficiaryService {
  private final BeneficiaryRepository beneficiaryRepository;
  private final UserRepository userRepository;

  public BeneficiaryService(
      BeneficiaryRepository beneficiaryRepository, UserRepository userRepository) {
    this.beneficiaryRepository = beneficiaryRepository;
    this.userRepository = userRepository;
  }

  public List<BeneficiaryResponse> list() {
    return beneficiaryRepository
        .findByUserEmailOrderByIdDesc(SecurityUtils.getCurrentUserEmail())
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public BeneficiaryResponse create(BeneficiaryRequest request) {
    User customer = getCurrentUser();
    String bankCode = normalizeBankCode(request.bankCode());
    if (beneficiaryRepository.existsByUserIdAndBankCodeAndAccountNumber(
        customer.getId(), bankCode, request.accountNumber())) {
      throw new ResourceAlreadyExistsException("Beneficiary already exists");
    }

    Beneficiary beneficiary = new Beneficiary();
    beneficiary.setUser(customer);
    applyRequest(beneficiary, request, bankCode);
    return toResponse(beneficiaryRepository.save(beneficiary));
  }

  @Transactional
  public BeneficiaryResponse update(Long beneficiaryId, BeneficiaryRequest request) {
    Beneficiary beneficiary = getOwnedBeneficiary(beneficiaryId);
    applyRequest(beneficiary, request, normalizeBankCode(request.bankCode()));
    return toResponse(beneficiaryRepository.save(beneficiary));
  }

  @Transactional
  public void delete(Long beneficiaryId) {
    beneficiaryRepository.delete(getOwnedBeneficiary(beneficiaryId));
  }

  private void applyRequest(
      Beneficiary beneficiary, BeneficiaryRequest request, String normalizedBankCode) {
    beneficiary.setBankCode(normalizedBankCode);
    beneficiary.setAccountNumber(request.accountNumber());
    beneficiary.setAccountName(request.accountName().trim());
    beneficiary.setNickname(request.nickname());
  }

  private Beneficiary getOwnedBeneficiary(Long beneficiaryId) {
    // Ownership is part of the query, so another customer's record is never exposed.
    return beneficiaryRepository
        .findByIdAndUserEmail(beneficiaryId, SecurityUtils.getCurrentUserEmail())
        .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found"));
  }

  private User getCurrentUser() {
    return userRepository
        .findByEmail(SecurityUtils.getCurrentUserEmail())
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
  }

  private String normalizeBankCode(String bankCode) {
    return bankCode.trim().toUpperCase();
  }

  private BeneficiaryResponse toResponse(Beneficiary beneficiary) {
    return new BeneficiaryResponse(
        beneficiary.getId(),
        beneficiary.getBankCode(),
        beneficiary.getAccountNumber(),
        beneficiary.getAccountName(),
        beneficiary.getNickname(),
        beneficiary.getCreatedAt());
  }
}
