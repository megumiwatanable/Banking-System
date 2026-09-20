package com.banking.beneficiary.infrastructure;

import com.banking.beneficiary.domain.Beneficiary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
  List<Beneficiary> findByUserEmailOrderByIdDesc(String email);

  Optional<Beneficiary> findByIdAndUserEmail(Long id, String email);

  boolean existsByUserIdAndBankCodeAndAccountNumber(Long userId, String bankCode, String accountNumber);
}
