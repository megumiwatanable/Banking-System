package com.banking.citad.infrastructure;

import com.banking.citad.domain.CitadInquiry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CitadInquiryRepository extends JpaRepository<CitadInquiry, Long> {
  List<CitadInquiry> findByUserEmailOrderByCreatedAtDesc(String email);

  Optional<CitadInquiry> findByIdAndUserEmail(Long id, String email);

  boolean existsByReferenceNumber(String referenceNumber);

  boolean existsByTransactionIdAndUserEmail(Long transactionId, String email);
}
