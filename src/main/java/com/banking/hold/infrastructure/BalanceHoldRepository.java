package com.banking.hold.infrastructure;

import com.banking.hold.domain.BalanceHold;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceHoldRepository extends JpaRepository<BalanceHold, Long> {
  List<BalanceHold> findByAccountUserEmailOrderByCreatedAtDesc(String email);

  Optional<BalanceHold> findByIdAndAccountUserEmail(Long id, String email);

  boolean existsByReference(String reference);
}
